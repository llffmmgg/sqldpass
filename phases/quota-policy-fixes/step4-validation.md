# Step 4: Validation

## Checklist

- Backend targeted quota tests pass.
- Frontend lint/build pass.
- Android assemble/unit tests pass where the local environment supports them.
- iOS code changes are reviewed for compile-level consistency; full build is deferred to macOS/Xcode.

## Manual Behavior Checks

- Opening a mock exam detail page does not consume the daily mock quota.
- Starting a non-past mock exam consumes one daily mock quota.
- A quota-exceeded mock start surfaces paywall UX and does not open cached content.
- Random questions display quota consumption based on returned count.
