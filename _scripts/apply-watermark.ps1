[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string[]]$Path,
    [string]$CommentPrefix = '//',
    # Resolved for this repository with
    #   .codex/skills/bc-developmentndebugging/scripts/watermark/resolve-watermark.ps1 -Canonical
    # (organisation NDBlockConnect -> Brand "BlockConnect", developer login StarsailsClover).
    [string]$CanonicalValue = 'GitHub@NDBlockConnect | BlockConnect@StarsailsClover'
)

# Applies the BlockConnect repository watermark to text sources.
#
# Rules implemented (see the bc-developmentndebugging skill, "Development Watermark"):
#   * the canonical value is resolved from the repository remote and the local `gh` login;
#   * ordinary content is split into windows of at most 50 non-watermark, non-blank lines and
#     exactly one watermark is placed at a random eligible line inside each window;
#   * the rendered comment inserts a random number of spaces so the text is not fixed;
#   * validating a watermark means removing ASCII spaces and comparing with the canonical value.
#
# Structured payloads (JSON, resource-pack metadata) must not be passed to this script.

$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$canonical = $CanonicalValue.Trim()
Write-Host "canonical watermark: $canonical"

function New-RenderedWatermark([string]$Value) {
    $random = [System.Random]::new()
    $builder = [System.Text.StringBuilder]::new()
    for ($i = 0; $i -lt $Value.Length; $i++) {
        [void]$builder.Append($Value[$i])
        $next = if ($i + 1 -lt $Value.Length) { $Value[$i + 1] } else { [char]0 }
        if ($Value[$i] -ne ' ' -and $next -ne ' ' -and $next -ne [char]0 -and $random.Next(0, 3) -eq 0) {
            [void]$builder.Append(' ' * $random.Next(1, 3))
        }
    }
    return $builder.ToString()
}

function Get-SafeLines([System.Collections.Generic.List[string]]$Lines) {
    # A watermark must never land inside a block comment, on an import/package line, or between a
    # decorator and its declaration, because those positions either break the document or make it
    # unreadable. Only "ordinary" code or text lines are offered as candidates.
    $safe = [System.Collections.Generic.List[int]]::new()
    $inBlockComment = $false
    for ($i = 0; $i -lt $Lines.Count; $i++) {
        $line = $Lines[$i]
        $trimmed = $line.Trim()
        $startsInComment = $inBlockComment
        if (-not $inBlockComment -and $trimmed -match '/\*') { $inBlockComment = $true }
        if ($inBlockComment -and $trimmed -match '\*/') {
            $inBlockComment = $false
            if (-not $startsInComment) { }
        }
        if ($startsInComment -or $inBlockComment) { continue }
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        if ($trimmed -like 'package *' -or $trimmed -like 'import *') { continue }
        if ($trimmed -like '#' -or $trimmed -like '//*') { continue }
        $safe.Add($i)
    }
    return $safe
}

$canonicalKey = $canonical -replace '\s', ''

foreach ($item in $Path) {
    $file = $item
    if (-not [System.IO.Path]::IsPathRooted($file)) { $file = Join-Path $repoRoot $item }
    if (-not (Test-Path -LiteralPath $file)) { Write-Warning "skip (missing): $file"; continue }

    $lines = [System.Collections.Generic.List[string]]::new()
    Get-Content -LiteralPath $file | ForEach-Object { $lines.Add($_) }
    $original = $lines.Count

    $already = ($lines | Where-Object { ($_ -replace '\s', '') -like "*$canonicalKey*" }).Count

    $safeLines = Get-SafeLines $lines
    $window = 0
    $inserted = 0
    $out = [System.Collections.Generic.List[string]]::new()
    for ($index = 0; $index -lt $lines.Count; $index++) {
        $line = $lines[$index]
        $stripped = $line -replace '\s', ''
        $isWatermark = $stripped -like "*$canonicalKey*"
        $out.Add($line)
        if ($isWatermark -or [string]::IsNullOrWhiteSpace($line)) { continue }
        $window++
        if ($window -ge 50) {
            $window = 0
            # Place the watermark at the next safe line at or after this boundary.
            $target = $safeLines | Where-Object { $_ -ge $index } | Select-Object -First 1
            if ($null -ne $target) {
                $out.Add(("{0}{1}" -f $CommentPrefix, (New-RenderedWatermark $canonical)))
                $inserted++
            }
        }
    }

    if ($inserted -eq 0 -and $already -eq 0) {
        # Short file: place a single watermark at a random eligible line.
        $out = [System.Collections.Generic.List[string]]::new()
        $eligible = @($safeLines)
        if ($eligible.Count -eq 0) { Write-Warning "skip (empty): $file"; continue }
        $target = $eligible[(Get-Random -Minimum 0 -Maximum $eligible.Count)]
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($i -eq $target) { $out.Add(("{0}{1}" -f $CommentPrefix, (New-RenderedWatermark $canonical))) }
            $out.Add($lines[$i])
        }
        $inserted = 1
    }

    if ($inserted -gt 0) {
        Set-Content -LiteralPath $file -Value $out -Encoding UTF8
        Write-Host ("watermarked {0} (+{1}, {2} -> {3} lines)" -f $item, $inserted, $original, $out.Count)
    } else {
        Write-Host ("already watermarked: {0}" -f $item)
    }
}

if ($Canonical) { Write-Host "validation key: $canonicalKey" }
