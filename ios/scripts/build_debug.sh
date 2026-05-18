#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

xcodebuild \
  -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
