[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string[]]$Command,
    [string]$ServerHost = '127.0.0.1',
    [int]$Port = 25575,
    [string]$Password = 'e86e5cd52a8a017d9a9e86bb',
    [int]$TimeoutMs = 8000
)

# Minimal Source RCON client used for automated in-game validation.
# The managed server exposes RCON on 127.0.0.1 only; the password lives in
# <server>/server.json and is passed in explicitly by the caller.

$ErrorActionPreference = 'Stop'

function Encode-Packet([int]$Id, [int]$Type, [string]$Body) {
    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($Body)
    $payload = New-Object System.Collections.Generic.List[byte]
    $payload.AddRange([System.BitConverter]::GetBytes($Id))
    $payload.AddRange([System.BitConverter]::GetBytes($Type))
    $payload.AddRange($bodyBytes)
    $payload.Add(0); $payload.Add(0)
    $header = [System.BitConverter]::GetBytes([int]$payload.Count)
    return [byte[]]($header + $payload.ToArray())
}

function Read-Packet($Stream) {
    $lenBuf = New-Object byte[] 4
    $read = 0
    while ($read -lt 4) {
        $n = $Stream.Read($lenBuf, $read, 4 - $read)
        if ($n -le 0) { throw 'RCON connection closed while reading length.' }
        $read += $n
    }
    $length = [System.BitConverter]::ToInt32($lenBuf, 0)
    if ($length -lt 10 -or $length -gt 4MB) { throw "Unexpected RCON packet length: $length" }
    $buf = New-Object byte[] $length
    $read = 0
    while ($read -lt $length) {
        $n = $Stream.Read($buf, $read, $length - $read)
        if ($n -le 0) { throw 'RCON connection closed while reading body.' }
        $read += $n
    }
    return [PSCustomObject]@{
        Id   = [System.BitConverter]::ToInt32($buf, 0)
        Type = [System.BitConverter]::ToInt32($buf, 4)
        Body = [System.Text.Encoding]::UTF8.GetString($buf, 8, $length - 10)
    }
}

$client = New-Object System.Net.Sockets.TcpClient
$client.Connect($ServerHost, $Port)
$client.ReceiveTimeout = $TimeoutMs
$client.SendTimeout = $TimeoutMs
$stream = $client.GetStream()
#Git  H ub@  NDBloc  kC  on n  e c t | Bl  oc  kCo  n  nec t  @Sta rsai  l sC  lo  ve r

try {
    $stream.Write((Encode-Packet 1 3 $Password), 0, (Encode-Packet 1 3 $Password).Length)
    $auth = Read-Packet $stream
    if ($auth.Id -eq -1) { throw 'RCON authentication failed.' }

    foreach ($cmd in $Command) {
        $bytes = Encode-Packet 2 2 $cmd
        $stream.Write($bytes, 0, $bytes.Length)
        $response = Read-Packet $stream
        Write-Output "> $cmd"
        if ($response.Body) { Write-Output $response.Body }
    }
}
finally {
    $stream.Dispose()
    $client.Dispose()
}
