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

$toolInput = $payload.tool_input
$eventName = [string]$payload.hook_event_name
$command = ""

if ($null -ne $toolInput) {
  if ($toolInput.PSObject.Properties.Name -contains "command") {
    $command = [string]$toolInput.command
  }
  elseif ($toolInput.PSObject.Properties.Name -contains "cmd") {
    $command = [string]$toolInput.cmd
  }
}

if ([string]::IsNullOrWhiteSpace($command)) {
  exit 0
}

$blockedPattern = "rm\s+-rf|git\s+push\s+--force|git\s+reset\s+--hard|DROP\s+TABLE|Remove-Item\s+.*-Recurse\s+.*-Force"

if ($command -match $blockedPattern) {
  if ($eventName -eq "PermissionRequest") {
    $response = @{
      hookSpecificOutput = @{
        hookEventName = "PermissionRequest"
        decision = @{
          behavior = "deny"
          message = "Destructive command blocked by repository hook."
        }
      }
    }
  }
  else {
    $response = @{
      hookSpecificOutput = @{
        hookEventName = "PreToolUse"
        permissionDecision = "deny"
        permissionDecisionReason = "Destructive command blocked by repository hook."
      }
    }
  }

  $response | ConvertTo-Json -Depth 5 -Compress
  exit 0
}

exit 0
