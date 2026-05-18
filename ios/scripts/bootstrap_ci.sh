#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v xcodegen >/dev/null 2>&1; then
  brew install xcodegen
fi

xcodegen generate

if grep -q "objectVersion = 77;" Sqldpass.xcodeproj/project.pbxproj; then
  perl -0pi -e 's/objectVersion = 77;/objectVersion = 56;/' Sqldpass.xcodeproj/project.pbxproj
fi
