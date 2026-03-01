param(
    [int]$Port = 18080,
    [int]$StartupTimeoutSeconds = 45
)

$ErrorActionPreference = "Stop"

$args = @(
    "-q",
    "-pl", "sqm-middleware-rest",
    "-am",
    "org.springframework.boot:spring-boot-maven-plugin:3.5.11:run",
    "-Dspring-boot.run.arguments=--server.port=$Port",
    "-Dsqm.middleware.rest.security.apiKeyEnabled=false",
    "-Dsqm.middleware.rest.abuse.rateLimitEnabled=false"
)

$process = Start-Process -FilePath "mvn" -ArgumentList $args -PassThru -NoNewWindow

try {
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    $ready = $false
    while ((Get-Date) -lt $deadline) {
        if ($process.HasExited) {
            throw "REST smoke check failed: Maven process exited early with code $($process.ExitCode)."
        }
        try {
            $health = Invoke-RestMethod -Method Get -Uri "http://localhost:$Port/sqm/middleware/v1/health"
            $readiness = Invoke-RestMethod -Method Get -Uri "http://localhost:$Port/sqm/middleware/v1/readiness"
            if ($health.status -eq "UP" -and ($readiness.status -eq "READY" -or $readiness.status -eq "NOT_READY")) {
                $ready = $true
                break
            }
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }

    if (-not $ready) {
        throw "REST smoke check failed: host did not become healthy within $StartupTimeoutSeconds seconds."
    }

    Write-Host "REST smoke check passed on port $Port."
}
finally {
    if (-not $process.HasExited) {
        Stop-Process -Id $process.Id -Force
    }
}
