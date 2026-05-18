# Step 1 — iOS 오프라인 큐 partial retry

## 작업 디렉터리
`ios/`

## 배경 / Why
- `ios/Sqldpass/Core/Persistence/SolveSyncManager.swift` 의 `tryDrain()` 은 한 row submit 이 실패하면 `break` 로 전체 drain 을 중단 (line ~43).
- 시나리오: 10개 큐 중 5번째가 서버 5xx 받으면 6~10 도 시도 안 됨 → 사용자 진행 막힘.
- iOS 결제·인증과 달리 solve 제출은 idempotent (`clientSubmissionId` 가드) — 개별 실패가 다른 row 처리 막을 이유 없음.
- Android `AppRepository.drainPendingSolves` (line 207-210) 가 이미 같은 방식으로 partial retry 처리 중. iOS 만 맞추면 됨.

## 변경 대상

### 1. `ios/Sqldpass/Core/Persistence/SolveSyncManager.swift`
- `tryDrain()` 의 for-loop 내 catch 절에서 `break` 를 제거하고 다음 row 로 continue.
- 단 인증 실패(401) 는 break 가 맞음 — 로그아웃 후 큐 처리 의미 없음. APIError 분기:
  ```swift
  catch APIError.unauthorized, APIError.forbidden {
      // 인증 만료 — 더 시도해봤자 모두 실패. break.
      break
  } catch {
      // 일시 오류 — 다음 row 시도. 실패 카운트 증가.
      failureCount += 1
      continue
  }
  ```
- `lastDrainAt`, `lastDrainFailedCount`, `lastDrainSyncedCount` 같은 published 프로퍼티 추가 (`@Published var ... = 0`).
- `tryDrain()` 종료 시 위 카운트 갱신.

### 2. `ios/Sqldpass/Features/Solo/Components/OfflineQueueChip.swift` (있다면)
- 현재 "Offline Queue: N" 표시 — 실패 카운트도 함께 보여주는 게 자연스러움. 단 본 phase 의 핵심은 동작 수정이라 UI 변경은 최소. 옵션:
  - chip 텍스트에 실패 N건 있을 때 색/문구 변경.
  - 또는 본 phase 에서는 ObservableObject 만 노출하고 UI 활용은 후속.
- 둘 중 가벼운 쪽 선택. 본문 동작 검증 가능한 한 줄 변경이면 OK.

### 3. (선택) `SolveSyncManager` 의 ObservableObject 노출
- 본 클래스가 이미 `@MainActor` 라면 `ObservableObject` 채택하고 `@Published` 프로퍼티 사용.
- 아닌 경우 그냥 `@MainActor` 클래스로 두고 외부에서 `objectWillChange` 패턴 안 써도 됨 — 본 변경의 핵심은 동작 수정.

## 작업 절차
1. SolveSyncManager.swift 전체 읽고 현 구조 파악.
2. for-loop 의 break 를 인증 실패만 break / 나머지는 continue + failureCount++ 로 분기.
3. 카운트 published 프로퍼티 추가 + 종료 시 갱신.
4. OfflineQueueChip 등 사용처가 카운트 사용하면 보강. 안 하면 그대로.
5. macOS 빌드 검증은 Windows 세션이라 미수행 — 사용자가 별도 검증.

## 검증
- Windows 빌드 불가. 사용자가 macOS 에서:
  ```bash
  cd ios
  xcodebuild -project Sqldpass.xcodeproj -scheme Sqldpass \\
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \\
    -configuration Debug build
  ```

## 금지사항
- 401/403 도 continue 처리 금지. 이유: 토큰 만료된 상태에서 큐 전체 시도하면 모든 row 가 401 반환 → 의미 없는 API 호출 폭증. 인증 실패는 break.
- failureCount 가 무한히 누적되도록 두지 말 것. 이유: drain 마다 0 으로 리셋해야 마지막 drain 결과를 정확히 반영.
- SwiftData 트랜잭션 분리/변경 금지. 이유: 본 phase 는 retry 로직만 다룸.

## 산출물
- 수정 파일 목록 + 한 줄 설명.
- macOS 빌드 미수행 명시.
