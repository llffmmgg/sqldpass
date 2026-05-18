# Step 3 — Dialog/Sheet/Dropdown 호출처 치환

## 배경

Step 2 의 신규 primitive 3종을 기존 Material3 모달 호출처에 적용. AlertDialog 8회 + ModalBottomSheet 1회 + DropdownMenu 6회 총 15곳.

Step 2 의존 + Step 4 와 병렬 가능.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/`

## 변경 대상

### AlertDialog 8회 → AppDialog

| 파일 | 라인 | 컨텍스트 |
|---|---|---|
| `ui/dashboard/NicknameEditDialog.kt:26` | 닉네임 편집 (입력 다이얼로그) | `AppDialog(title="닉네임 변경", content = { AppTextField(...) }, confirmLabel="저장", onConfirm = ..., dismissLabel="취소")` |
| `ui/profile/ProfileTab.kt:114` | pendingNotice (안내) | `AppDialog(title="알림", message=msg, dismissLabel="확인", onDismissAction = { pendingNotice = null })` — confirm 없음 |
| `ui/profile/ProfileTab.kt:426` | FeedbackDialog confirm | 동일 패턴 + content slot 으로 OutlinedTextField → AppTextField |
| `ui/solve/SoloSolveScreen.kt:267` | 종료 확인 | `AppDialog(title="풀이 종료", message="지금까지의 진행은 저장되지 않습니다.", confirmLabel="종료", onConfirm = onExit, destructive=true, dismissLabel="계속 풀기")` |
| `ui/runner/QuestionRunnerScreen.kt:264` | 종료 확인 | 동일 |
| `ui/runner/ReportDialog.kt:44` | 신고 다이얼로그 | content slot 으로 입력 + 라벨 |
| `ui/runner/RunnerJumpGrid.kt:53` | 점프 그리드 | 큰 컨텐츠 — AppDialog(content) 또는 AppBottomSheet 둘 다 가능. 본 step 은 AppDialog 권장 (기존 동작 유지) |

### ModalBottomSheet 1회 → AppBottomSheet

| 파일 | 라인 |
|---|---|
| `ui/home/CertInfoSheet.kt:37,41` | rememberModalBottomSheetState + ModalBottomSheet → AppBottomSheet |

### DropdownMenu 6회 → AppDropdown

| 파일 | 라인 | 컨텍스트 |
|---|---|---|
| `ui/solve/SoloSolveScreen.kt:360,361` | SoloProgressHeader 또는 동등 위치 메뉴 | `AppDropdown(expanded, onDismiss) { AppDropdownItem("이 문제 신고", icon=Report, onClick=onReport) }` |
| `ui/runner/QuestionRunnerScreen.kt:382,387,402` | 운영 메뉴 (3 아이템) | 동일 패턴 |
| `ui/home/HomeScreen.kt:190,191` (HomeAccountMenu) | 계정 메뉴 (로그아웃) | 동일 |

## Acceptance Criteria

1. 15곳 모두 Material3 AlertDialog/ModalBottomSheet/DropdownMenu import 제거
2. 각 호출이 AppDialog/AppBottomSheet/AppDropdown 사용
3. 기능 동작 회귀 0 (확인/취소 / dismiss / 메뉴 선택 흐름 그대로)
4. `cd mobile; .\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

수동 시나리오:
- 닉네임 편집 → 입력 → 저장 → dismiss
- 풀이 화면 종료 다이얼로그 → 종료(destructive 톤) / 계속
- 홈 계정 메뉴 → 로그아웃
- 자격증 카드 → 시트 (CertInfoSheet)
- 신고 다이얼로그 입력 → 전송

## 금지 사항

- AppDialog/Sheet/Dropdown 의 시그니처를 본 step 에서 변경하지 마라. 이유: Step 2 의 산출물. 변경이 필요하면 Step 2 로 회귀.
- 기능 동작 변경 금지 (예: 종료 확인이 즉시 종료로 바뀐다거나). 이유: 본 step 은 visible chrome 만.
- 새 AlertDialog/ModalBottomSheet/DropdownMenu 호출 추가 금지 — 항상 App* primitive 사용.

## Status 규칙

- 성공: step 3 `completed`.
- 실패: 3회 후 `error`.
