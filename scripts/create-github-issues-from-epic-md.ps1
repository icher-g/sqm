param(
    [Parameter(Mandatory = $true)]
    [string]$Path,
    [string]$Repo = "icher-g/sqm",
    [switch]$SkipStories,
    [switch]$WhatIf
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' is not available in PATH."
    }
}

function Extract-Section {
    param(
        [string]$Text,
        [string]$Header,
        [string[]]$NextHeaders
    )

    $NextHeaders = @($NextHeaders)
    $escapedHeader = [regex]::Escape($Header)
    if ($NextHeaders.Count -eq 0) {
        $pattern = '(?ms)^' + $escapedHeader + '\s*\r?\n(.*)$'
    }
    else {
        $next = ($NextHeaders | ForEach-Object { [regex]::Escape($_) }) -join '|'
        $pattern = '(?ms)^' + $escapedHeader + '\s*\r?\n(.*?)(?=^(?:' + $next + ')\s*$|\z)'
    }

    $m = [regex]::Match($Text, $pattern)
    if (-not $m.Success) {
        return ""
    }
    return $m.Groups[1].Value.Trim()
}

function Parse-Labels {
    param([string]$Text)

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return @()
    }

    $clean = $Text -replace '`', ''
    return ($clean -split '[,\r\n]' |
        ForEach-Object { $_.Trim() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
        Select-Object -Unique)
}

function Get-ExistingLabels {
    param([string]$RepoName)

    $args = @('label', 'list', '--limit', '1000', '--json', 'name', '--jq', '.[].name')
    if (-not [string]::IsNullOrWhiteSpace($RepoName)) {
        $args += @('--repo', $RepoName)
    }

    $out = & gh @args
    return @($out -split '\r?\n' |
        ForEach-Object { $_.Trim() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
        Select-Object -Unique)
}

function Ensure-Labels {
    param(
        [string[]]$Labels,
        [string]$RepoName,
        [switch]$PreviewOnly
    )

    $normalized = @($Labels | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)
    if ($normalized.Count -eq 0) {
        return
    }

    if ($PreviewOnly) {
        Write-Host "[WhatIf] ensure labels: $($normalized -join ', ')"
        return
    }

    $existing = Get-ExistingLabels -RepoName $RepoName
    foreach ($label in $normalized) {
        if ($existing -contains $label) {
            continue
        }

        $args = @('label', 'create', $label, '--color', 'BFD4F2', '--description', 'Auto-created by epic issue script')
        if (-not [string]::IsNullOrWhiteSpace($RepoName)) {
            $args += @('--repo', $RepoName)
        }

        & gh @args | Out-Null
        Write-Host "Created missing label: $label"
        $existing += $label
    }
}

function Get-IssueByExactTitle {
    param(
        [string]$Title,
        [string]$RepoName
    )

    $args = @('issue', 'list', '--state', 'all', '--limit', '1000', '--search', $Title, '--json', 'number,title,url')
    if (-not [string]::IsNullOrWhiteSpace($RepoName)) {
        $args += @('--repo', $RepoName)
    }

    $json = & gh @args
    if ([string]::IsNullOrWhiteSpace($json)) {
        return $null
    }

    $parsed = $json | ConvertFrom-Json
    $issues = @()
    if ($null -eq $parsed) {
        $issues = @()
    }
    elseif ($parsed -is [System.Array]) {
        $issues = @($parsed)
    }
    elseif ($parsed.PSObject.Properties.Name -contains 'title') {
        $issues = @($parsed)
    }

    return ($issues | Where-Object { $_.PSObject.Properties.Name -contains 'title' -and $_.title -eq $Title } | Select-Object -First 1)
}

function Create-Issue {
    param(
        [string]$Title,
        [string]$Body,
        [string[]]$Labels,
        [string]$RepoName,
        [switch]$PreviewOnly
    )

    $labels = @($Labels | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)

    $existingIssue = $null
    if (-not $PreviewOnly) {
        $existingIssue = Get-IssueByExactTitle -Title $Title -RepoName $RepoName
        if ($null -ne $existingIssue) {
            Write-Host "Reusing existing issue #$($existingIssue.number): $($existingIssue.url)"
            return $existingIssue.url
        }
    }

    Ensure-Labels -Labels $labels -RepoName $RepoName -PreviewOnly:$PreviewOnly

    if ($PreviewOnly) {
        $previewExisting = Get-IssueByExactTitle -Title $Title -RepoName $RepoName
        if ($null -ne $previewExisting) {
            Write-Host "[WhatIf] reuse existing issue #$($previewExisting.number): $($previewExisting.url)"
            return $previewExisting.url
        }
        Write-Host "[WhatIf] gh issue create --title '$Title' --labels '$($labels -join ',')'"
        return ""
    }

    $args = @('issue', 'create', '--title', $Title, '--body', $Body)
    if (-not [string]::IsNullOrWhiteSpace($RepoName)) {
        $args += @('--repo', $RepoName)
    }

    foreach ($label in $labels) {
        $args += @('--label', $label)
    }

    return (& gh @args)
}

Require-Command -Name 'gh'

$resolvedPath = Resolve-Path -Path $Path
$raw = Get-Content -Path $resolvedPath -Raw

$epicTitle = [regex]::Match($raw, '(?ms)^## Epic\s*\r?\n.*?^### Title\s*\r?\n\s*`([^`]+)`').Groups[1].Value.Trim()
if ([string]::IsNullOrWhiteSpace($epicTitle)) {
    throw "Could not parse epic title from: $resolvedPath"
}

$problem = Extract-Section -Text $raw -Header '### Problem Statement' -NextHeaders @('### Epic Goal', '### Business Value', '### Definition of Done', '### Suggested Labels')
$goal = Extract-Section -Text $raw -Header '### Epic Goal' -NextHeaders @('### Business Value', '### Definition of Done', '### Suggested Labels')
$value = Extract-Section -Text $raw -Header '### Business Value' -NextHeaders @('### Definition of Done', '### Suggested Labels')
$dod = Extract-Section -Text $raw -Header '### Definition of Done' -NextHeaders @('### Suggested Labels')
$epicLabelsText = Extract-Section -Text $raw -Header '### Suggested Labels' -NextHeaders @('---', '## User Stories')
$epicLabels = @(Parse-Labels -Text $epicLabelsText)
if ($epicLabels.Count -eq 0) {
    $epicLabels = @('epic')
}

$epicBody = @"
## Problem Statement
$problem

## Epic Goal
$goal

## Business Value
$value

## Definition of Done
$dod

Source: $resolvedPath
"@

$epicUrl = Create-Issue -Title $epicTitle -Body $epicBody -Labels $epicLabels -RepoName $Repo -PreviewOnly:$WhatIf
$epicNumber = ''
if ($epicUrl -match '/issues/(\d+)') {
    $epicNumber = $Matches[1]
    if (-not $WhatIf) {
        Write-Host "Using epic issue #${epicNumber}: $epicUrl"
    }
}
elseif ($WhatIf) {
    Write-Host "[WhatIf] Epic parsed: $epicTitle"
}

if ($SkipStories) {
    return
}

$storyMatches = [regex]::Matches($raw, '(?ms)^### Story\s+([^\r\n]+)\s*\r?\n(.*?)(?=^---\s*$|^### Story\s+|\z)')
foreach ($storyMatch in $storyMatches) {
    $storyId = $storyMatch.Groups[1].Value.Trim()
    $block = $storyMatch.Groups[2].Value

    $titleMatch = [regex]::Match($block, '(?ms)^#### Title\s*\r?\n\s*`([^`]+)`')
    if (-not $titleMatch.Success) {
        continue
    }
    $storyTitle = $titleMatch.Groups[1].Value.Trim()

    $userStory = Extract-Section -Text $block -Header '#### User Story' -NextHeaders @('#### Acceptance Criteria', '#### Labels', '#### Depends On')
    $acceptance = Extract-Section -Text $block -Header '#### Acceptance Criteria' -NextHeaders @('#### Labels', '#### Depends On')
    $labelsText = Extract-Section -Text $block -Header '#### Labels' -NextHeaders @('#### Depends On')
    $dependsOn = Extract-Section -Text $block -Header '#### Depends On' -NextHeaders @()

    $storyLabels = @(Parse-Labels -Text $labelsText)
    if ($storyLabels.Count -eq 0) {
        $storyLabels = @('story')
    }

    $epicLinkLine = if ([string]::IsNullOrWhiteSpace($epicNumber)) { "Epic: $epicTitle" } else { "Epic: #$epicNumber" }

    $storyBody = @"
Story ID: $storyId

## User Story
$userStory

## Acceptance Criteria
$acceptance

## Depends On
$dependsOn

$epicLinkLine

Source: $resolvedPath
"@

    $storyUrl = Create-Issue -Title $storyTitle -Body $storyBody -Labels $storyLabels -RepoName $Repo -PreviewOnly:$WhatIf
    if (-not $WhatIf -and -not [string]::IsNullOrWhiteSpace($storyUrl)) {
        Write-Host "Using story issue: $storyUrl"
    }
}
