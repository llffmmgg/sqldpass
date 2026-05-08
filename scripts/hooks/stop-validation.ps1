param(
  [switch]$FrontendOnly,
  [switch]$BackendOnly
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$failed = $false
$messages = New-Object System.Collections.Generic.List[string]

function Invoke-CommandInDir {
  param(
    [Parameter(Mandatory = $true)][string]$Directory,
    [Parameter(Mandatory = $true)][string]$Command,
    [Parameter(Mandatory = $true)][string[]]$Arguments
  )

  Push-Location (Join-Path $repoRoot $Directory)
  try {
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
      throw "$Directory command failed: $Command $($Arguments -join ' ')"
    }
  }
  finally {
    Pop-Location
  }
}

try {
  if (-not $BackendOnly) {
    Invoke-CommandInDir -Directory "frontend" -Command "npm" -Arguments @("run", "lint")
    Invoke-CommandInDir -Directory "frontend" -Command "npm" -Arguments @("run", "build")
  }
}
catch {
  $failed = $true
  $messages.Add($_.Exception.Message)
}

try {
  if (-not $FrontendOnly) {
    Invoke-CommandInDir -Directory "backend" -Command ".\gradlew.bat" -Arguments @("test")
  }
}
catch {
  $failed = $true
  $messages.Add($_.Exception.Message)
}

if ($failed) {
  $response = @{
    continue = $false
    stopReason = "Monorepo validation failed."
    systemMessage = ($messages -join "`n")
  }
  $response | ConvertTo-Json -Depth 4 -Compress
  exit 0
}

exit 0
