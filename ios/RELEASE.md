# iOS Release

This app is intentionally wired to stop before TestFlight upload by default.

## GitHub Actions

`iOS CI` runs on PR and main pushes for `ios/**` changes:

```bash
bash ios/scripts/bootstrap_ci.sh
bash ios/scripts/build_debug.sh
```

`iOS Release` is manual (`workflow_dispatch`). It imports signing assets, archives the Release app, exports an IPA, and uploads the IPA as a workflow artifact.

The `upload_to_testflight` input defaults to `false`. Leave it false until the IPA has been checked on macOS/Xcode.

## Required Secrets

- `APPLE_TEAM_ID`
- `IOS_CERTIFICATE_P12_BASE64`
- `IOS_CERTIFICATE_PASSWORD`
- `IOS_PROVISIONING_PROFILE_BASE64`
- `IOS_PROVISIONING_PROFILE_NAME`
- `IOS_KEYCHAIN_PASSWORD`

For TestFlight upload:

- `APP_STORE_CONNECT_KEY_ID`
- `APP_STORE_CONNECT_ISSUER_ID`
- `APP_STORE_CONNECT_API_KEY_BASE64`

## Optional Variables

- `IOS_BUNDLE_ID`, default `com.sqldpass.app`
- `IOS_EXPORT_METHOD`, default `app-store-connect`

If Xcode rejects `app-store-connect` for `method`, set `IOS_EXPORT_METHOD` to `app-store` in repository variables.

## Local macOS Check

From `ios/`:

```bash
bash scripts/bootstrap_ci.sh
bash scripts/build_debug.sh
```

To export an IPA locally, provide the same environment variables used by GitHub Actions and run:

```bash
bash scripts/archive_app_store.sh
```

To upload after checking the IPA:

```bash
bash scripts/upload_testflight.sh build/export/*.ipa
```
