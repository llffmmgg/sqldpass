param(
  [switch]$All,
  [switch]$Frontend,
  [switch]$Backend
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$safeDir = "C:/Users/admin/Desktop/sqldpass/sqldpass"
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

function Get-ChangedFiles {
  Push-Location $repoRoot
  try {
    $files = @()
    $files += git -c safe.directory=$safeDir diff --name-only
    $files += git -c safe.directory=$safeDir diff --cached --name-only
    $files += git -c safe.directory=$safeDir ls-files --others --exclude-standard
    return $files | Where-Object { $_ } | Sort-Object -Unique
  }
  finally {
    Pop-Location
  }
}

$changedFiles = Get-ChangedFiles

$runFrontend = $All -or $Frontend -or ($changedFiles | Where-Object { $_ -like "frontend/*" })
$runBackend = $All -or $Backend -or ($changedFiles | Where-Object { $_ -like "backend/*" })

if (-not $runFrontend -and -not $runBackend) {
  exit 0
}

try {
  if ($runFrontend) {
    Invoke-CommandInDir -Directory "frontend" -Command "npm" -Arguments @("run", "lint")
    Invoke-CommandInDir -Directory "frontend" -Command "npm" -Arguments @("run", "build")
  }
}
catch {
  $failed = $true
  $messages.Add($_.Exception.Message)
}

try {
  if ($runBackend) {
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
    stopReason = "Lint/build/test hook failed."
    systemMessage = ($messages -join "`n")
  }
  $response | ConvertTo-Json -Depth 4 -Compress
  exit 0
}

exit 0
