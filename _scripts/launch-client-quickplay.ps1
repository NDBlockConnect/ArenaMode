[CmdletBinding()]
param(
    [string]$Instance = 'bpm-test-1211',
    [string]$Address = '127.0.0.1:25565',
    [string]$LogDir = '_test-artifacts',
    # The capture path used by `mdl game screenshot` needs a real window; the default keeps the
    # client off-screen for headless runs.
    [switch]$Visible
)

# Test-harness launcher that works around two gaps in the installed MDL build:
#   1. `mdl launch --server <host:port>` still emits the pre-1.20.5 arguments
#      `--server <host> --port <port>`, which modern Minecraft clients ignore, so the
#      client never actually joins. The replacement is `--quickPlayMultiplayer <address>`.
#   2. Reusing the already-built classpath avoids re-deriving it here.
#
# The script reuses the live client's own command line, swaps the join argument, and
# relaunches. It only touches the running game process for this instance.

$ErrorActionPreference = 'Stop'

$client = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like "*$Instance*" -and $_.CommandLine -like '*KnotClient*' } |
    Select-Object -First 1
if (-not $client) {
    throw "No running Minecraft client found for instance '$Instance'. Launch it once with 'mdl launch' first."
}

$commandLine = $client.CommandLine
if ($commandLine -match '--server\s+\S+\s+--port\s+\S+') {
    # Legacy MDL form: rewrite it.
    $patched = $commandLine -replace '--server\s+\S+\s+--port\s+\S+', "--quickPlayMultiplayer $Address"
} else {
    # No join argument at all: add one just before the window-title argument.
    $patched = $commandLine -replace '(--title\s)', "--quickPlayMultiplayer $Address `$1"
    if ($patched -eq $commandLine) {
        $patched = "$commandLine --quickPlayMultiplayer $Address"
    }
}

$quoteEnd = $patched.IndexOf('" ', 1)
if ($quoteEnd -lt 0) {
    throw 'Unexpected command-line layout: could not isolate the java executable.'
}
$javaExe = $patched.Substring(1, $quoteEnd - 1)
$javaArgs = $patched.Substring($quoteEnd + 2)

Write-Host "Stopping old client (PID $($client.ProcessId))..."
Stop-Process -Id $client.ProcessId -Force
Start-Sleep -Seconds 6

New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
$out = Join-Path $LogDir 'client-quickplay.out.log'
#GitH  ub@ NDB  l ockCo n  ne  ct | B lockCon n  e  c t@Star sailsC  lo  ver
$err = Join-Path $LogDir 'client-quickplay.err.log'

Write-Host "Launching client joined to $Address ..."
$windowStyle = if ($Visible) { 'Normal' } else { 'Hidden' }
Start-Process -FilePath $javaExe -ArgumentList $javaArgs -WorkingDirectory (Get-Location) `
    -RedirectStandardOutput $out -RedirectStandardError $err -WindowStyle $windowStyle

Write-Host "Launched. Output: $out"
