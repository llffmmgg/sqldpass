#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

IPA_PATH="${1:-}"
if [[ -z "$IPA_PATH" ]]; then
  echo "Usage: scripts/upload_testflight.sh path/to/app.ipa" >&2
  exit 2
fi

require_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: $name" >&2
    exit 2
  fi
}

require_env APP_STORE_CONNECT_KEY_ID
require_env APP_STORE_CONNECT_ISSUER_ID
require_env APP_STORE_CONNECT_API_KEY_BASE64

KEY_DIR="$HOME/.appstoreconnect/private_keys"
KEY_PATH="$KEY_DIR/AuthKey_$APP_STORE_CONNECT_KEY_ID.p8"

mkdir -p "$KEY_DIR"
printf '%s' "$APP_STORE_CONNECT_API_KEY_BASE64" | base64 -D > "$KEY_PATH"
chmod 600 "$KEY_PATH"

xcrun altool \
  --upload-app \
  --type ios \
  --file "$IPA_PATH" \
  --apiKey "$APP_STORE_CONNECT_KEY_ID" \
  --apiIssuer "$APP_STORE_CONNECT_ISSUER_ID"
