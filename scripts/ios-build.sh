#!/usr/bin/env bash
# sqldpass iOS 빌드/테스트 헬퍼 (macOS 전용)
# 사용법:
#   scripts/ios-build.sh build         # 시뮬레이터용 빌드
#   scripts/ios-build.sh test          # 단위/UI 테스트 실행
#   scripts/ios-build.sh run           # 빌드 후 시뮬레이터에서 실행
#   scripts/ios-build.sh clean         # DerivedData 정리
#   scripts/ios-build.sh devices       # 사용 가능한 시뮬레이터 목록
#   scripts/ios-build.sh beta          # fastlane 으로 TestFlight 업로드
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOS_DIR="$REPO_ROOT/ios"
PROJECT="$IOS_DIR/Sqldpass.xcodeproj"
SCHEME="Sqldpass"
SIM_DEVICE="${SIM_DEVICE:-iPhone 16 Pro}"
DESTINATION="platform=iOS Simulator,name=${SIM_DEVICE}"

require_macos() {
  if [[ "$(uname)" != "Darwin" ]]; then
    echo "[error] iOS 빌드는 macOS 에서만 가능합니다. 현재: $(uname)" >&2
    exit 1
  fi
}

require_xcode() {
  if ! command -v xcodebuild >/dev/null; then
    echo "[error] Xcode 가 설치되지 않았습니다. App Store 에서 Xcode 를 설치하세요." >&2
    exit 1
  fi
  if [[ ! -d "$PROJECT" ]]; then
    echo "[error] Xcode 프로젝트가 없습니다: $PROJECT" >&2
    echo "       Phase B (Xcode → File → New → Project) 를 먼저 수행하세요." >&2
    exit 1
  fi
}

cmd_build() {
  require_macos; require_xcode
  xcodebuild -project "$PROJECT" -scheme "$SCHEME" \
    -destination "$DESTINATION" -configuration Debug \
    clean build | xcpretty || true
}

cmd_test() {
  require_macos; require_xcode
  xcodebuild test -project "$PROJECT" -scheme "$SCHEME" \
    -destination "$DESTINATION" | xcpretty || true
}

cmd_run() {
  require_macos; require_xcode
  cmd_build
  echo "[info] 시뮬레이터에서 실행하려면 Xcode 를 열고 ⌘R 을 누르세요:"
  echo "       open $PROJECT"
}

cmd_clean() {
  rm -rf "$IOS_DIR/build" "$IOS_DIR/DerivedData" "$HOME/Library/Developer/Xcode/DerivedData/Sqldpass-"*
  echo "[info] DerivedData 정리 완료"
}

cmd_devices() {
  require_macos
  xcrun simctl list devices available | grep -E "iPhone|iPad"
}

cmd_beta() {
  require_macos; require_xcode
  if ! command -v fastlane >/dev/null; then
    echo "[error] fastlane 미설치. 'brew install fastlane' 후 재시도하세요." >&2
    exit 1
  fi
  cd "$IOS_DIR" && fastlane beta
}

case "${1:-}" in
  build)   cmd_build ;;
  test)    cmd_test ;;
  run)     cmd_run ;;
  clean)   cmd_clean ;;
  devices) cmd_devices ;;
  beta)    cmd_beta ;;
  *)
    echo "Usage: $0 {build|test|run|clean|devices|beta}"
    exit 1
    ;;
esac
