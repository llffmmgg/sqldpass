#!/usr/bin/env bash
set -euo pipefail

# Codex PreToolUse hook for TDD discipline.
# It inspects apply_patch/Edit/Write input and blocks implementation edits when
# no test file is touched in the same change and no matching test file exists.

payload="$(cat)"

if [[ -z "${payload//[[:space:]]/}" ]]; then
  exit 0
fi

event_name="$(printf '%s' "$payload" | sed -n 's/.*"hook_event_name"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"
tool_name="$(printf '%s' "$payload" | sed -n 's/.*"tool_name"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"

if [[ "$event_name" != "PreToolUse" ]]; then
  exit 0
fi

case "$tool_name" in
  apply_patch|Edit|Write) ;;
  *) exit 0 ;;
esac

command="$(printf '%s' "$payload" | sed -n 's/.*"command"[[:space:]]*:[[:space:]]*"\(.*\)"[[:space:]]*}.*/\1/p')"

if [[ -z "$command" ]]; then
  command="$payload"
fi

changed_files="$(
  printf '%s\n' "$command" |
    sed -n \
      -e 's/^+++ b\///p' \
      -e 's/^--- a\///p' \
      -e 's/^\*\*\* Add File: //p' \
      -e 's/^\*\*\* Update File: //p' \
      -e 's/^\*\*\* Delete File: //p' |
    sed 's#\\#/#g' |
    sort -u
)"

if [[ -z "$changed_files" ]]; then
  exit 0
fi

is_test_file() {
  local file="$1"
  [[ "$file" =~ (^|/)(__tests__|test|tests)/ ]] ||
  [[ "$file" =~ (^|/)[^/]*\.(test|spec)\.(ts|tsx|js|jsx)$ ]] ||
  [[ "$file" =~ (^|/)src/test/ ]] ||
  [[ "$file" =~ (^|/)Test\.java$ ]] ||
  [[ "$file" =~ (^|/)[^/]*Test\.java$ ]]
}

is_implementation_file() {
  local file="$1"
  [[ "$file" =~ ^frontend/src/.*\.(ts|tsx|js|jsx)$ ]] ||
  [[ "$file" =~ ^backend/src/main/java/.*\.java$ ]]
}

is_exempt_file() {
  local file="$1"
  [[ "$file" =~ ^docs/ ]] ||
  [[ "$file" =~ ^\.codex/ ]] ||
  [[ "$file" =~ ^\.claude/ ]] ||
  [[ "$file" =~ ^scripts/ ]] ||
  [[ "$file" =~ \.(md|mdx|json|toml|yaml|yml|css|svg|png|jpg|jpeg|gif)$ ]]
}

has_matching_test() {
  local file="$1"
  local normalized="${file//\\//}"

  if [[ "$normalized" =~ ^frontend/src/(.*)\.(ts|tsx|js|jsx)$ ]]; then
    local rel="${BASH_REMATCH[1]}"
    local dir
    local base
    dir="$(dirname "frontend/src/$rel")"
    base="$(basename "$rel")"
    compgen -G "$dir/$base.test.*" >/dev/null ||
    compgen -G "$dir/$base.spec.*" >/dev/null ||
    compgen -G "$dir/__tests__/$base.*" >/dev/null ||
    compgen -G "frontend/src/**/$base.test.*" >/dev/null ||
    compgen -G "frontend/src/**/$base.spec.*" >/dev/null
    return $?
  fi

  if [[ "$normalized" =~ ^backend/src/main/java/(.*)\.java$ ]]; then
    local rel="${BASH_REMATCH[1]}"
    local test_file="backend/src/test/java/${rel}Test.java"
    [[ -f "$test_file" ]]
    return $?
  fi

  return 1
}

test_touched=false
implementation_without_test=()

while IFS= read -r file; do
  [[ -z "$file" ]] && continue

  if is_test_file "$file"; then
    test_touched=true
    continue
  fi

  if is_exempt_file "$file"; then
    continue
  fi

  if is_implementation_file "$file" && ! has_matching_test "$file"; then
    implementation_without_test+=("$file")
  fi
done <<< "$changed_files"

if [[ "$test_touched" == true ]]; then
  exit 0
fi

if (( ${#implementation_without_test[@]} > 0 )); then
  reason="TDD Guard: implementation files are being changed without touching tests and no matching test file exists: ${implementation_without_test[*]}"
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"%s"}}\n' "$reason"
  exit 0
fi

exit 0
