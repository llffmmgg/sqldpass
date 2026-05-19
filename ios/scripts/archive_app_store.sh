#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: $name" >&2
    exit 2
  fi
}

require_env APPLE_TEAM_ID
require_env IOS_BUNDLE_ID
require_env IOS_CERTIFICATE_P12_BASE64
require_env IOS_CERTIFICATE_PASSWORD
require_env IOS_PROVISIONING_PROFILE_BASE64

IOS_EXPORT_METHOD="${IOS_EXPORT_METHOD:-app-store-connect}"
KEYCHAIN_PASSWORD="${KEYCHAIN_PASSWORD:-temporary-ios-build-keychain}"
BUILD_DIR="${BUILD_DIR:-build}"
ARCHIVE_PATH="$BUILD_DIR/Sqldpass.xcarchive"
EXPORT_PATH="$BUILD_DIR/export"
RUNNER_TEMP="${RUNNER_TEMP:-$BUILD_DIR/tmp}"
KEYCHAIN_PATH="$RUNNER_TEMP/sqldpass-ios-build.keychain-db"
CERTIFICATE_PATH="$RUNNER_TEMP/certificate.p12"
PROFILE_PATH="$RUNNER_TEMP/profile.mobileprovision"
PROFILE_PLIST="$RUNNER_TEMP/profile.plist"
EXPORT_OPTIONS="$RUNNER_TEMP/ExportOptions.plist"

mkdir -p "$RUNNER_TEMP" "$EXPORT_PATH"

printf '%s' "$IOS_CERTIFICATE_P12_BASE64" | base64 -D > "$CERTIFICATE_PATH"
printf '%s' "$IOS_PROVISIONING_PROFILE_BASE64" | base64 -D > "$PROFILE_PATH"

security create-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN_PATH"
security set-keychain-settings -lut 21600 "$KEYCHAIN_PATH"
security unlock-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN_PATH"
security import "$CERTIFICATE_PATH" -P "$IOS_CERTIFICATE_PASSWORD" -A -t cert -f pkcs12 -k "$KEYCHAIN_PATH"
security list-keychain -d user -s "$KEYCHAIN_PATH" $(security list-keychain -d user | sed s/\"//g)
security set-key-partition-list -S apple-tool:,apple: -s -k "$KEYCHAIN_PASSWORD" "$KEYCHAIN_PATH"

security cms -D -i "$PROFILE_PATH" > "$PROFILE_PLIST"
PROFILE_UUID=$(/usr/libexec/PlistBuddy -c 'Print UUID' "$PROFILE_PLIST")
PROFILE_NAME="${IOS_PROVISIONING_PROFILE_NAME:-$(/usr/libexec/PlistBuddy -c 'Print Name' "$PROFILE_PLIST")}"
mkdir -p "$HOME/Library/MobileDevice/Provisioning Profiles"
cp "$PROFILE_PATH" "$HOME/Library/MobileDevice/Provisioning Profiles/$PROFILE_UUID.mobileprovision"

cat > "$EXPORT_OPTIONS" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>method</key>
  <string>$IOS_EXPORT_METHOD</string>
  <key>signingStyle</key>
  <string>manual</string>
  <key>teamID</key>
  <string>$APPLE_TEAM_ID</string>
  <key>provisioningProfiles</key>
  <dict>
    <key>$IOS_BUNDLE_ID</key>
    <string>$PROFILE_NAME</string>
  </dict>
  <key>stripSwiftSymbols</key>
  <true/>
  <key>uploadBitcode</key>
  <false/>
  <key>uploadSymbols</key>
  <true/>
</dict>
</plist>
PLIST

rm -rf "$ARCHIVE_PATH" "$EXPORT_PATH"
mkdir -p "$EXPORT_PATH"

xcodebuild \
  -project Sqldpass.xcodeproj \
  -scheme Sqldpass \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath "$ARCHIVE_PATH" \
  DEVELOPMENT_TEAM="$APPLE_TEAM_ID" \
  PRODUCT_BUNDLE_IDENTIFIER="$IOS_BUNDLE_ID" \
  CODE_SIGN_STYLE=Manual \
  CODE_SIGN_IDENTITY="Apple Distribution" \
  archive

xcodebuild \
  -exportArchive \
  -archivePath "$ARCHIVE_PATH" \
  -exportPath "$EXPORT_PATH" \
  -exportOptionsPlist "$EXPORT_OPTIONS"

ls -la "$EXPORT_PATH"
