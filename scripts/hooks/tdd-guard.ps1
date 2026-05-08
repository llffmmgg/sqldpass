$ErrorActionPreference = "Stop"

$rawInput = [Console]::In.ReadToEnd()

if ([string]::IsNullOrWhiteSpace($rawInput)) {
  exit 0
}

try {
  $payload = $rawInput | ConvertFrom-Json
}
catch {
  exit 0
}

if ([string]$payload.hook_event_name -ne "PreToolUse") {
  exit 0
}

$toolName = [string]$payload.tool_name
if ($toolName -notmatch "^(apply_patch|Edit|Write)$") {
  exit 0
}

$command = ""
if ($null -ne $payload.tool_input) {
  if ($payload.tool_input.PSObject.Properties.Name -contains "command") {
    $command = [string]$payload.tool_input.command
  }
  elseif ($payload.tool_input.PSObject.Properties.Name -contains "path") {
    $command = [string]$payload.tool_input.path
  }
}

if ([string]::IsNullOrWhiteSpace($command)) {
  exit 0
}

$changedFiles = New-Object System.Collections.Generic.List[string]

foreach ($line in ($command -split "`r?`n")) {
  $file = $null

  if ($line -match "^\+\+\+ b/(.+)$") {
    $file = $Matches[1]
  }
  elseif ($line -match "^--- a/(.+)$") {
    $file = $Matches[1]
  }
  elseif ($line -match "^\*\*\* Add File: (.+)$") {
    $file = $Matches[1]
  }
  elseif ($line -match "^\*\*\* Update File: (.+)$") {
    $file = $Matches[1]
  }
  elseif ($line -match "^\*\*\* Delete File: (.+)$") {
    $file = $Matches[1]
  }
  elseif ($line -match "^(frontend|backend)[\\/].+") {
    $file = $line
  }

  if ($file) {
    $changedFiles.Add(($file -replace "\\", "/"))
  }
}

$changedFiles = $changedFiles | Sort-Object -Unique

if (-not $changedFiles -or $changedFiles.Count -eq 0) {
  exit 0
}

function Test-IsTestFile {
  param([string]$File)

  return (
    $File -match "(^|/)(__tests__|test|tests)/" -or
    $File -match "(^|/)[^/]*\.(test|spec)\.(ts|tsx|js|jsx)$" -or
    $File -match "(^|/)src/test/" -or
    $File -match "(^|/)[^/]*Test\.java$"
  )
}

function Test-IsImplementationFile {
  param([string]$File)

  return (
    $File -match "^frontend/src/.*\.(ts|tsx|js|jsx)$" -or
    $File -match "^backend/src/main/java/.*\.java$"
  )
}

function Test-IsExemptFile {
  param([string]$File)

  return (
    $File -match "^docs/" -or
    $File -match "^\.codex/" -or
    $File -match "^\.claude/" -or
    $File -match "^scripts/" -or
    $File -match "\.(md|mdx|json|toml|yaml|yml|css|svg|png|jpg|jpeg|gif)$"
  )
}

function Test-HasMatchingTest {
  param([string]$File)

  if ($File -match "^frontend/src/(.*)\.(ts|tsx|js|jsx)$") {
    $rel = $Matches[1]
    $dir = Split-Path "frontend/src/$rel" -Parent
    $base = Split-Path $rel -Leaf

    $patterns = @(
      "$dir/$base.test.*",
      "$dir/$base.spec.*",
      "$dir/__tests__/$base.*",
      "frontend/src/**/$base.test.*",
      "frontend/src/**/$base.spec.*"
    )

    foreach ($pattern in $patterns) {
      if (Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue | Select-Object -First 1) {
        return $true
      }
    }

    return $false
  }

  if ($File -match "^backend/src/main/java/(.*)\.java$") {
    $rel = $Matches[1]
    $testFile = "backend/src/test/java/${rel}Test.java"
    return Test-Path $testFile
  }

  return $false
}

$testTouched = $false
$implementationWithoutTest = New-Object System.Collections.Generic.List[string]

foreach ($file in $changedFiles) {
  if (Test-IsTestFile $file) {
    $testTouched = $true
    continue
  }

  if (Test-IsExemptFile $file) {
    continue
  }

  if ((Test-IsImplementationFile $file) -and -not (Test-HasMatchingTest $file)) {
    $implementationWithoutTest.Add($file)
  }
}

if ($testTouched) {
  exit 0
}

if ($implementationWithoutTest.Count -gt 0) {
  $reason = "TDD Guard: implementation files are being changed without touching tests and no matching test file exists: $($implementationWithoutTest -join ', ')"
  $response = @{
    hookSpecificOutput = @{
      hookEventName = "PreToolUse"
      permissionDecision = "deny"
      permissionDecisionReason = $reason
    }
  }

  $response | ConvertTo-Json -Depth 5 -Compress
  exit 0
}

exit 0
