param(
    [string]$ToolName = "middleware.analyze",
    [string]$Sql = "select id from users",
    [string]$Dialect = "postgresql",
    [ValidateSet("ANALYZE", "EXECUTE")]
    [string]$Mode = "ANALYZE",
    [ValidateSet("OFF", "BIND")]
    [string]$ParameterizationMode = "OFF"
)

$ErrorActionPreference = "Stop"

function New-FramedJson {
    param([Parameter(Mandatory = $true)][string]$Json)

    $bytes = [System.Text.Encoding]::UTF8.GetByteCount($Json)
    return "Content-Length: $bytes`r`n`r`n$Json"
}

function Read-McpResponse {
    param([Parameter(Mandatory = $true)]$Reader)

    $contentLength = -1
    while ($true) {
        $line = $Reader.ReadLine()
        if ($null -eq $line) {
            return $null
        }
        if ($line -eq "") {
            break
        }
        if ($line.StartsWith("Content-Length:", [System.StringComparison]::OrdinalIgnoreCase)) {
            $contentLength = [int]($line.Substring("Content-Length:".Length).Trim())
        }
    }

    if ($contentLength -lt 0) {
        throw "Missing Content-Length header in MCP response."
    }

    $buffer = New-Object char[] $contentLength
    $read = 0
    while ($read -lt $contentLength) {
        $chunk = $Reader.Read($buffer, $read, $contentLength - $read)
        if ($chunk -le 0) {
            throw "Unexpected EOF while reading MCP response body."
        }
        $read += $chunk
    }

    return -join $buffer
}

$requests = @(
    @{ Id = 1; Json = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' },
    @{ Id = 2; Json = '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' },
    @{ Id = 3; Json = (
        '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{' +
        '"name":"' + $ToolName + '",' +
        '"arguments":{' +
        '"sql":' + (ConvertTo-Json $Sql -Compress) + ',' +
        '"context":{' +
        '"dialect":' + (ConvertTo-Json $Dialect -Compress) + ',' +
        '"mode":' + (ConvertTo-Json $Mode -Compress) + ',' +
        '"parameterizationMode":' + (ConvertTo-Json $ParameterizationMode -Compress) +
        '}' +
        '}' +
        '}}'
    )}
)

$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = "mvn"
$startInfo.Arguments = "-q -pl sqm-middleware-mcp -am exec:java -Dexec.mainClass=io.sqm.middleware.mcp.SqlMiddlewareMcpApplication"
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.WorkingDirectory = (Get-Location).Path

$process = New-Object System.Diagnostics.Process
$process.StartInfo = $startInfo
$null = $process.Start()

Start-Sleep -Milliseconds 1500

try {
    $stdin = $process.StandardInput
    $stdout = $process.StandardOutput

    foreach ($req in $requests) {
        $frame = New-FramedJson -Json $req.Json
        $stdin.Write($frame)
        $stdin.Flush()

        $response = Read-McpResponse -Reader $stdout
        Write-Host "== Response for id=$($req.Id) =="
        Write-Host $response
        Write-Host
    }

    $exitNotification = New-FramedJson -Json '{"jsonrpc":"2.0","method":"exit"}'
    $stdin.Write($exitNotification)
    $stdin.Flush()
}
finally {
    $stderr = $process.StandardError.ReadToEnd()
    if (-not [string]::IsNullOrWhiteSpace($stderr)) {
        Write-Host "== MCP STDERR =="
        Write-Host $stderr
    }
    if (-not $process.HasExited) {
        $process.Kill()
    }
}
