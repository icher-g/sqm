param(
    [string]$PipeInName = "sqm-mcp-in",
    [string]$PipeOutName = "sqm-mcp-out"
)

$ErrorActionPreference = "Stop"

Write-Host "Starting MCP named-pipe bridge..."
Write-Host "  Inbound pipe : \\.\pipe\$PipeInName (client writes requests)"
Write-Host "  Outbound pipe: \\.\pipe\$PipeOutName (client reads responses)"

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

$pipeIn = [System.IO.Pipes.NamedPipeServerStream]::new(
    $PipeInName,
    [System.IO.Pipes.PipeDirection]::In,
    1,
    [System.IO.Pipes.PipeTransmissionMode]::Byte,
    [System.IO.Pipes.PipeOptions]::Asynchronous
)

$pipeOut = [System.IO.Pipes.NamedPipeServerStream]::new(
    $PipeOutName,
    [System.IO.Pipes.PipeDirection]::Out,
    1,
    [System.IO.Pipes.PipeTransmissionMode]::Byte,
    [System.IO.Pipes.PipeOptions]::Asynchronous
)

try {
    Write-Host "Waiting for client connection on inbound pipe..."
    $pipeIn.WaitForConnection()
    Write-Host "Inbound pipe connected."

    Write-Host "Waiting for client connection on outbound pipe..."
    $pipeOut.WaitForConnection()
    Write-Host "Outbound pipe connected."

    $stdinStream = $process.StandardInput.BaseStream
    $stdoutStream = $process.StandardOutput.BaseStream

    $forwardInTask = [System.Threading.Tasks.Task]::Run({
        param($source, $target)
        $buffer = New-Object byte[] 4096
        while ($true) {
            $read = $source.Read($buffer, 0, $buffer.Length)
            if ($read -le 0) {
                break
            }
            $target.Write($buffer, 0, $read)
            $target.Flush()
        }
    }, @($pipeIn, $stdinStream))

    $forwardOutTask = [System.Threading.Tasks.Task]::Run({
        param($source, $target)
        $buffer = New-Object byte[] 4096
        while ($true) {
            $read = $source.Read($buffer, 0, $buffer.Length)
            if ($read -le 0) {
                break
            }
            $target.Write($buffer, 0, $read)
            $target.Flush()
        }
    }, @($stdoutStream, $pipeOut))

    Write-Host "Bridge is running. Press Ctrl+C to stop."

    while (-not $process.HasExited -and -not $forwardInTask.IsCompleted -and -not $forwardOutTask.IsCompleted) {
        Start-Sleep -Milliseconds 500
    }
}
finally {
    try {
        if (-not $process.HasExited) {
            $process.Kill()
        }
    }
    catch {
    }

    $stderr = $process.StandardError.ReadToEnd()
    if (-not [string]::IsNullOrWhiteSpace($stderr)) {
        Write-Host "== MCP STDERR =="
        Write-Host $stderr
    }

    if ($pipeIn) { $pipeIn.Dispose() }
    if ($pipeOut) { $pipeOut.Dispose() }
}
