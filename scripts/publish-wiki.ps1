param(
    [string]$WikiRepoUrl = "https://github.com/icher-g/sqm.wiki.git",
    [string]$SourceDir = "wiki-src",
    [string]$WorkDir = ".wiki-tmp",
    [string]$CommitMessage = "Update wiki pages"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path $SourceDir)) {
    throw "Source directory not found: $SourceDir"
}

if (Test-Path $WorkDir) {
    Remove-Item -Recurse -Force $WorkDir
}

git clone $WikiRepoUrl $WorkDir
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $WorkDir)) {
    throw "Failed to clone wiki repository. Verify wiki is enabled and URL is correct: $WikiRepoUrl"
}

$wikiRoot = Resolve-Path $WorkDir
$sourceRoot = Resolve-Path $SourceDir

Get-ChildItem -LiteralPath $wikiRoot -Force |
    Where-Object { $_.Name -ne ".git" } |
    Remove-Item -Recurse -Force

Copy-Item -Path (Join-Path $sourceRoot "*") -Destination $wikiRoot -Recurse -Force

Push-Location $WorkDir
try {
    git add .
    $changes = git status --porcelain
    if (-not $changes) {
        Write-Host "No wiki changes to publish."
        exit 0
    }
    git commit -m $CommitMessage
    git push
    Write-Host "Wiki published to $WikiRepoUrl"
}
finally {
    Pop-Location
}
