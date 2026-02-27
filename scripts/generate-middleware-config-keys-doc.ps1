param(
    [string]$SourcePath = "sqm-control/src/main/java/io/sqm/control/ConfigKeys.java",
    [string]$OutputPath = "docs/MIDDLEWARE_CONFIG_KEYS.md"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path $SourcePath)) {
    throw "Source file not found: $SourcePath"
}

$source = Get-Content -Raw -Path $SourcePath
$pattern = 'public\s+static\s+final\s+Key\s+(\w+)\s*=\s*Key\.of\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\);'
$matches = [regex]::Matches($source, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)

if ($matches.Count -eq 0) {
    throw "No key definitions found in $SourcePath"
}

$rows = foreach ($m in $matches) {
    [pscustomobject]@{
        Constant = $m.Groups[1].Value
        Property = $m.Groups[2].Value
        Environment = $m.Groups[3].Value
    }
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Middleware Configuration Keys")
$lines.Add("")
$lines.Add("> Auto-generated from `sqm-control/src/main/java/io/sqm/control/ConfigKeys.java`.")
$lines.Add("> Do not edit manually; run `scripts/generate-middleware-config-keys-doc.ps1`.")
$lines.Add("")
$lines.Add("| Constant | JVM Property | Environment Variable |")
$lines.Add("|---|---|---|")
foreach ($row in $rows) {
    $lines.Add("| ``$($row.Constant)`` | ``$($row.Property)`` | ``$($row.Environment)`` |")
}
$lines.Add("")

$dir = Split-Path -Parent $OutputPath
if ($dir -and -not (Test-Path $dir)) {
    New-Item -ItemType Directory -Path $dir | Out-Null
}

Set-Content -Path $OutputPath -Value $lines -Encoding utf8
Write-Host "Generated $OutputPath with $($rows.Count) keys."

