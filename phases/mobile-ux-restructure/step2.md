# Step 2 — Android 5탭 재편

## 배경

현재 Android BOTTOM_TABS: `Home / MockExam / WrongAnswers / Insights / Profile`.
신규 BOTTOM_TABS: `Home / MockExam / PastExam / SoloSolve / Profile`.

변경 요약:
- **제거**: WrongAnswers · Insights (라우트는 유지하되 탭에서 빠짐 — 내정보 메뉴에서 진입)
- **신규 탭**: PastExam · SoloSolve (둘 다 라우트는 이미 존재 — 탭 등록만)
- **유지**: Home / MockExam / Profile

MockExamTab 의 기출복원 세그먼트는 PastExam 탭 신설로 인해 제거 (mobile-5tab-redesign step 2 의 세그먼트 흡수를 되돌림). 모의고사 탭은 회차 카탈로그 단일 책임.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/`

## 변경 대상

| 파일 | 변경 |
|---|---|
| `nav/SqldpassNav.kt` | `BOTTOM_TABS` 5개 교체 — Home/MockExam/PastExam/SoloSolve/Profile. 라벨: "홈"/"모의고사"/"기출복원"/"실전 문제"/"마이". 아이콘: `Home`/`Quiz`/`History`/`PlayCircleOutline`/`PersonOutline` (적당히 — UI 폴리시는 사용자가). |
| `MainActivity.kt` | SoloSolve 라우트의 transition 을 `tabFadeEnter/Exit` 로 변경(현재 pushSlide). NavHost 매핑에서 PastExam 도 동일 tabFade. Home 의 "10문제 풀기" CTA 가 호출하는 startSoloSolve 진입점은 SoloSolve 탭이 정문이 되도록 navigate(SoloSolve.route). |
| `ui/mockexam/MockExamTab.kt` | 기출복원 세그먼트 코드 제거 — `SegmentedButtonRow`, `PastExamList` 분기, `onSelectCert`/`onStartPastExam` props 정리. MockExamTab 은 모의고사 회차만 표시. |
| `ui/solve/SolveTab.kt` | 실전 문제 탭의 진입 화면으로 정착 — 자격증 칩 + 과목 카드 구조 그대로. onStartPractice 가 SoloSolve 라우트 진입은 기존 그대로. |

## Acceptance Criteria

1. `cd mobile; .\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL.
2. 하단 5탭이 새 순서로 노출 — 홈/모의고사/기출복원/실전 문제/마이.
3. WrongAnswers 탭·Insights 탭 아이콘이 하단에서 사라짐. 라우트 자체는 `navController.navigate("wrong-answers")` 가 동작 (내정보 안에서 호출 예정 — step 4).
4. 모의고사 탭 진입 시 회차 목록만 노출. 기출복원 세그먼트 자취 없음.
5. 기출복원 탭 진입 시 PastExamTab 의 기존 구현 표시(자격증 칩 + 회차 카드). MockExamTab 의 onStartPastExam 콜백은 더 이상 호출되지 않음.
6. 실전 문제 탭(SolveTab) 진입 시 자격증 칩 + 과목 카드 노출, 카드 탭 시 SoloSolveScreen push.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

수동 시나리오:
- 5탭 모두 진입 → 각 탭 첫 화면 정상.
- 모의고사 탭 → 회차 카드만 (세그먼트 없음).
- 기출복원 탭 → 회차 카드.
- 실전 문제 탭 → 과목 카드.

## 금지 사항

- WrongAnswerTab.kt / InsightsTab.kt 의 화면 내용을 변경하지 마라. 이유: step 4 의 내정보 메뉴에서 이 화면들로 진입. 본 step 은 탭 배치 변경만.
- PastExamTab.kt 의 구현을 손대지 마라. 이유: 이미 동작 중인 화면. 본 step 은 탭에 등록만.
- Home/Profile 화면 본체는 본 step 에서 손대지 마라. 이유: 각각 step 3 / step 4 에서.
- BOTTOM_TABS 순서를 위 spec 과 다르게 정하지 마라. 이유: docs/MOBILE_UX_SPEC.md 의 좌→우 순서가 양 플랫폼 일치 약속.

## Status 규칙

- 성공: index.json step 2 `completed`, summary.
- 실패: 3회 후 `error`.
