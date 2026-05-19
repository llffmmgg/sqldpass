#!/usr/bin/env python3
"""App Store Connect 에서 같은 marketing version train 의 latest build +1 을 stdout 으로 출력.

Usage:
    python3 next_build_number.py <bundle_id> <marketing_version>

Env (required):
    APP_STORE_CONNECT_KEY_ID
    APP_STORE_CONNECT_ISSUER_ID
    APP_STORE_CONNECT_API_KEY_BASE64   # base64 of the .p8 private key file contents

같은 marketing version 안의 build number 만 단조 증가해야 하므로 train 별로 +1 한다.
해당 train 에 빌드가 하나도 없으면 1 을 반환.
"""
from __future__ import annotations

import base64
import json
import os
import sys
import time
import urllib.parse
import urllib.request

try:
    import jwt  # PyJWT
except ImportError:
    sys.stderr.write("[next_build_number] PyJWT not installed. Run: pip3 install 'pyjwt[crypto]'\n")
    sys.exit(2)


API_ROOT = "https://api.appstoreconnect.apple.com"


def fail(msg: str) -> "NoReturn":
    sys.stderr.write(f"[next_build_number] {msg}\n")
    sys.exit(1)


def make_token(key_id: str, issuer_id: str, p8_pem: str) -> str:
    now = int(time.time())
    payload = {
        "iss": issuer_id,
        "iat": now,
        "exp": now + 20 * 60,
        "aud": "appstoreconnect-v1",
    }
    headers = {"kid": key_id, "typ": "JWT"}
    return jwt.encode(payload, p8_pem, algorithm="ES256", headers=headers)


def api_get(path: str, params: dict, token: str) -> dict:
    qs = urllib.parse.urlencode(params, safe="[],")
    url = f"{API_ROOT}{path}?{qs}"
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.load(resp)
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        fail(f"HTTP {e.code} {e.reason} on {url}\n{body}")
    except urllib.error.URLError as e:
        fail(f"URLError on {url}: {e.reason}")


def main() -> None:
    if len(sys.argv) != 3:
        fail("Usage: next_build_number.py <bundle_id> <marketing_version>")
    bundle_id, marketing_version = sys.argv[1], sys.argv[2]

    key_id = os.environ.get("APP_STORE_CONNECT_KEY_ID")
    issuer_id = os.environ.get("APP_STORE_CONNECT_ISSUER_ID")
    key_b64 = os.environ.get("APP_STORE_CONNECT_API_KEY_BASE64")
    if not (key_id and issuer_id and key_b64):
        fail("missing env: APP_STORE_CONNECT_KEY_ID/ISSUER_ID/API_KEY_BASE64")

    try:
        p8_pem = base64.b64decode(key_b64).decode("utf-8")
    except Exception as e:
        fail(f"failed to base64-decode APP_STORE_CONNECT_API_KEY_BASE64: {e}")

    token = make_token(key_id, issuer_id, p8_pem)

    apps = api_get(
        "/v1/apps",
        {"filter[bundleId]": bundle_id, "fields[apps]": "bundleId"},
        token,
    )
    data = apps.get("data") or []
    if not data:
        fail(f"no app found for bundleId={bundle_id}")
    app_id = data[0]["id"]

    builds = api_get(
        "/v1/builds",
        {
            "filter[app]": app_id,
            "filter[preReleaseVersion.version]": marketing_version,
            "sort": "-version",
            "limit": "1",
            "fields[builds]": "version",
        },
        token,
    )
    bdata = builds.get("data") or []
    if not bdata:
        latest = 0
    else:
        try:
            latest = int(bdata[0]["attributes"]["version"])
        except (KeyError, TypeError, ValueError) as e:
            fail(f"unexpected build payload: {bdata[0]!r} ({e})")

    print(latest + 1)


if __name__ == "__main__":
    main()
