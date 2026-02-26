param(
    [string]$PipeInName = "sqm-mcp-in",
    [string]$PipeOutName = "sqm-mcp-out",
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
    param([Parameter(Mandatory = $true)][System.IO.StreamReader]$Reader)

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
    '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}',
    '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}',
    (
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
    )
)

$pipeInClient = [System.IO.Pipes.NamedPipeClientStream]::new(".", $PipeInName, [System.IO.Pipes.PipeDirection]::Out)
$pipeOutClient = [System.IO.Pipes.NamedPipeClientStream]::new(".", $PipeOutName, [System.IO.Pipes.PipeDirection]::In)

try {
    $pipeInClient.Connect(10000)
    $pipeOutClient.Connect(10000)

    $writer = [System.IO.StreamWriter]::new($pipeInClient, [System.Text.Encoding]::UTF8)
    $writer.AutoFlush = $true
    $reader = [System.IO.StreamReader]::new($pipeOutClient, [System.Text.Encoding]::UTF8)

    foreach ($json in $requests) {
        $frame = New-FramedJson -Json $json
        $writer.Write($frame)

        $response = Read-McpResponse -Reader $reader
        Write-Host "== Response =="
        Write-Host $response
        Write-Host
    }

    $writer.Write((New-FramedJson -Json '{"jsonrpc":"2.0","method":"exit"}'))
}
finally {
    if ($pipeInClient) { $pipeInClient.Dispose() }
    if ($pipeOutClient) { $pipeOutClient.Dispose() }
}
