# Step 5 — Android 오프라인 답안 큐잉 단일 채점 모드 확장 (android-offline-solve-queue)

## 배경

Android 는 이미 모의고사 응시용 오프라인 큐(`mobile/.../data/local/PendingSolveEntity.kt` + `OfflineSyncManager`)를 갖고 있다. 본 step 은 step 3 에서 만든 단일 채점 모드(`SoloSolveScreen`)의 1문제 1제출 흐름도 동일 큐를 거치도록 통합하고, 화면에 미동기화 상태 인디케이터를 추가한다.

핵심 변경 포인트:

1. 단일 풀이 제출도 `PendingSolveEntity` 의 row 로 enqueue.
2. `OfflineSyncManager` 가 단일 모드 row 도 drain (subjectId + 단일 answer 기반 `POST /api/solves`).
3. `SoloSolveScreen` 좌하단에 미동기화 카운트 인디케이터 (`pending count > 0` 일 때).

## 작업 디렉터리

```bash
cd mobile
```

## 사전 조사 (작업 시작 시 먼저 확인)

다음 파일을 grep / 읽기로 현 상태 파악 후 변경 계획 확정:

- `mobile/app/src/main/java/com/sqldpass/app/data/local/PendingSolveEntity.kt` — 컬럼 (특히 `submissionMode` 또는 `kind` 컬럼이 있는지)
- `mobile/app/src/main/java/com/sqldpass/app/data/local/OfflineDao.kt` (또는 비슷한 이름)
- `mobile/app/src/main/java/com/sqldpass/app/data/AppRepository.kt` 의 `submitSolveOffline()` / `enqueue*()` 메서드
- `mobile/app/src/main/java/com/sqldpass/app/data/sync/OfflineSyncManager.kt` (또는 비슷한)
- `mobile/app/src/main/java/com/sqldpass/app/data/local/SqldpassDatabase.kt` — Room 버전 및 마이그레이션 위치

위 파일들의 실제 구조에 따라 다음 중 하나 선택:

- **옵션 A — 컬럼 추가 없이 통합**: 기존 PendingSolveEntity 가 이미 subjectId/answers/totalCount 를 갖고 있으면 단일 모드는 `totalCount = 1` + answers 배열 1개로 그대로 enqueue. Room 마이그레이션 불필요.
- **옵션 B — `submissionKind` 컬럼 신설**: 만약 entity 가 mockExamId NOT NULL 같은 제약을 갖고 있으면 nullable 화 + `kind: "MOCK_EXAM" | "SOLO"` 컬럼 추가. Flyway 가 아닌 Room migration 작성 필요(`SqldpassDatabase` 버전 증가).

Acceptance Criteria 검증 시 옵션 A/B 중 어떤 경로를 택했는지 step summary 에 명시.

## 변경 대상 (옵션 A 가정 — 옵션 B 면 마이그레이션 추가)

| 파일 | 변경 |
|---|---|
| `mobile/.../data/AppRepository.kt` | `submitSoloSolveOffline(subjectId: Long, questionId: Long, selectedOption: Int?, answerText: String?)` 추가. 내부적으로 PendingSolve enqueue + 백그라운드 sync 시도 |
| `mobile/.../data/sync/OfflineSyncManager.kt` | drain 로직이 mockExamId null + totalCount=1 케이스를 단일 채점 POST 로 처리하도록 분기 추가. 멱등키 `clientSubmissionId` 유지 |
| `mobile/.../ui/solve/SoloSolveViewModel.kt` (step 3 산출물) | submit() 내부에서 직접 `submitSoloSolveOffline()` 호출. 성공/실패와 무관하게 다음 문제로 진행 가능 |
| `mobile/.../ui/solve/components/OfflineQueueChip.kt` | 신규 — 좌하단 작은 칩. `pending count > 0` 일 때만 노출. "오프라인 — N개 보관 중" |
| `mobile/.../ui/solve/SoloSolveScreen.kt` (step 3 산출물) | OfflineQueueChip 을 본문 하단 또는 진행 바 아래에 배치 |
| `mobile/.../ui/AppViewModel.kt` | `pendingSolveCount: Int` StateFlow 노출 (DAO observe) |

## 미동기화 인디케이터 위치

```
[상단 헤더]
[진행 바]
[오프라인 큐 칩] ← 미동기화 row > 0 일 때만, 진행 바 바로 아래 좌측 정렬
[문제 카드]
...
```

색: `Color.semanticWarning.copy(alpha = 0.12f)` 배경 + `Color.semanticWarning` 텍스트 + `CloudOff` 아이콘. 8dp 패딩.

문구: `오프라인 — ${count}개 보관 중` (count 1 일 때도 "1개").

## 네트워크 복귀 감지

`ConnectivityManager.NetworkCallback` 으로 `onAvailable` 시 `OfflineSyncManager.tryDrain()` 호출. 이미 있으면 그대로 사용, 없으면 신설.

```kotlin
class NetworkAvailabilityObserver(
    private val context: Context,
    private val onAvailable: () -> Unit,
) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { onAvailable() }
    }
    fun start() { cm.registerDefaultNetworkCallback(callback) }
    fun stop() { cm.unregisterNetworkCallback(callback) }
}
```

`MainActivity.onCreate` 에서 한 번 등록, `onDestroy` 에서 해제.

## Acceptance Criteria

1. `cd mobile; .\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
2. `cd mobile; .\gradlew.bat :app:testDebugUnitTest` → 통과. AppRepository / OfflineSyncManager 단일 모드 분기에 대한 최소 unit test 1개 추가(예: enqueue → drain mock → POST 호출 횟수 검증).
3. 비행기 모드 ON → SoloSolveScreen 에서 옵션 선택 → "정답 확인" → 정답 공개 + "오프라인 — 1개 보관 중" 칩 노출 → 다음 문제 진행 가능 → 비행기 모드 OFF → 약 1~3초 안에 칩 자동 사라짐.
4. 같은 문제를 반복 풀이로 N번 enqueue 했을 때 서버에는 멱등키로 중복 row 없이 N번 row 만 생성 (기존 모의고사 모드의 멱등성 그대로 활용).
5. 옵션 A 선택 시 Room 마이그레이션 추가 없음. 옵션 B 선택 시 SqldpassDatabase 버전 증가 + migration 코드 + summary 에 명시.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

수동 시나리오(에뮬레이터 또는 실기기):

1. 앱 진입 + Home → "10문제 풀기" → 옵션 선택 + 정답 확인 (정상 sync 됨 확인).
2. 비행기 모드 ON → 옵션 선택 + 정답 확인 → 칩 노출, 풀이 계속 진행.
3. 5문제 풀이 후 비행기 모드 OFF → 칩 자동 사라짐 + 서버 풀이 기록 5개 확인.

## 금지 사항

- 서버 API 계약을 본 step 에서 변경하지 마라. 이유: 본 phase 는 클라이언트만. `POST /api/solves` 기존 형식 유지.
- 단일 모드용 별도 endpoint 를 만들지 마라. 이유: 백엔드는 mockExamId 없는 단순 단일 풀이를 이미 받음(`subjectId + answers[1]` 형식).
- 미동기화 인디케이터에 "에러" 톤(danger 색) 을 쓰지 마라. 이유: 사용자에게 실패가 아닌 "보관 중" 상태로 인지시키는 게 본 phase 의 UX 원칙(SOLVE_SCREEN_SPEC.md 의 큐잉 정책).
- `submitSoloSolveOffline` 의 응답을 await 한 결과를 UI 진행 흐름에 사용하지 마라. 이유: 사용자는 정답 공개 직후 즉시 "다음 문제" 로 진행해야 하며, 큐잉은 백그라운드.
- 풀이 결과 화면(`SessionComplete`)에서 미동기화 항목이 있을 때 "제출 실패" 같은 경고 모달을 띄우지 마라. 이유: 동일 — 백그라운드 보관 흐름.

## Status 규칙

- 성공: index.json step 5 `completed`, summary 에 옵션 A/B 선택 + 빌드/테스트 결과 + 수동 시나리오 결과.
- 실패: 3회 시도 후 `error` + `error_message`.
