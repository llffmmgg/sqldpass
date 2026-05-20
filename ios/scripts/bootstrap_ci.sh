#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v xcodegen >/dev/null 2>&1; then
  brew install xcodegen
fi

# project.yml 의 targets.Sqldpass.configFiles.Release: Signing.xcconfig 는 generate 시점에
# 파일 존재를 검증한다 — 없으면 "Spec validation error". archive_app_store.sh 가 실 signing
# 값으로 덮어쓰기 전, simulator/CI 빌드는 빈 placeholder 만으로도 통과 (signing 무관).
if [[ ! -f Signing.xcconfig ]]; then
  cat > Signing.xcconfig <<'XCCONFIG'
// Placeholder. archive_app_store.sh 가 실 signing 값을 동적 inject 한다.
// simulator/CI 빌드는 본 파일을 사용하지 않으므로 빈 채로 두어도 무방.
XCCONFIG
fi

xcodegen generate

if ! grep -q "CODE_SIGN_ENTITLEMENTS = Sqldpass/Sqldpass.entitlements" Sqldpass.xcodeproj/project.pbxproj; then
  echo "[error] CODE_SIGN_ENTITLEMENTS was not generated for the Sqldpass target" >&2
  exit 1
fi

if grep -q "objectVersion = 77;" Sqldpass.xcodeproj/project.pbxproj; then
  perl -0pi -e 's/objectVersion = 77;/objectVersion = 56;/' Sqldpass.xcodeproj/project.pbxproj
fi
