# Step 6 — iOS 홈 정보 구조 재배치

## 배경

step 5 에서 신설한 `Features/Home/HomeView.swift` 의 빈 스텁을 본 step 에서 본격 구성. Android step 3 와 동등 위계:
1. 인사말 헤더
2. 스트릭 카드 (위험 톤 분기)
3. 이어풀기 추천 카드 (마지막 mode 분기)
4. 자격증 6종 ScrollView(.horizontal) 캐러셀
5. 자격증 카드 탭 → `.sheet(.medium) CertInfoSheet`

기존 `DashboardView.swift` 의 출석체크 패널 / 통계 그리드 / 모의고사·오답노트 빠른시작 카드는 본 phase 에서 제거 — 홈은 위 4섹션만.

macOS 검증 대기.

## 작업 디렉터리

`ios/`

## 변경 대상

| 파일 | 변경 |
|---|---|
| `Features/Home/HomeView.swift` | step 5 의 스텁을 본 구성으로 교체. ScrollView + VStack. DashboardViewModel 재사용(또는 신규 HomeViewModel — 단순) — streak, lastCert/Mode, member 필드. |
| `Features/Home/Components/StreakCard.swift` (신규) | currentStreak + 위험 톤 분기 (`streak.solvedToday == false && lastSolvedDate <= 어제`). |
| `Features/Home/Components/ContinueLastCard.swift` (신규) | lastCert/lastMode → 카드 텍스트 + onClick 시 selectTab 으로 push (mock_exams/past_exams/solo_solve). MainTabView 의 selection binding 사용. |
| `Features/Home/Components/CertCarousel.swift` (신규) | ScrollView(.horizontal) + 6 카드. tap → onCertTap 콜백. |
| `Features/Home/Components/CertInfoSheet.swift` (신규) | View — 시험 정보 4종 List + "PASS+ 소개" Button. Button 탭 시 dismiss + (별 동선) PaywallView 또는 ProfileView → PASS+. |
| `Core/Catalog/CertCatalog.swift` (신규) | 자격증 6종 정적 데이터 (시행처/문항수/시간/합격기준). Android `ui/home/CertCatalog.kt` 와 동일 값. |
| `Features/Dashboard/DashboardView.swift` | 본 step 에서 **삭제** — 더 이상 사용처 없음. 또는 코드 그대로 두되 사용 안 함(추후 정리). |
| `Features/Dashboard/DashboardViewModel.swift` | HomeView 가 그대로 사용한다면 유지, 아니면 미사용 — 정리는 별 step. |

## Acceptance Criteria (macOS 환경)

1. `xcodebuild ... build` → BUILD SUCCEEDED.
2. 홈 탭 진입 시 위→아래 4섹션 노출.
3. 스트릭 카드: 오늘 풀이 안 했고 lastSolvedDate ≤ 어제 → warning 톤 ("오늘 자정이 끝나면 …"). 그 외 normal.
4. 이어풀기 카드 탭 → 마지막 mode 별 탭으로 selection 전환 + 적절한 자격증/회차 pre-select.
5. 자격증 카드 탭 → `.sheet(.presentationDetents([.medium]))` CertInfoSheet 표시.
6. CertInfoSheet 의 "PASS+ 소개" CTA 탭 → ProfileView 또는 PaywallView 로 진입.

## 검증

```bash
cd ios
~/bin/xcodegen generate
xcodebuild ... build
```

**Windows 환경에서 빌드 불가** — 코드만 작성, 검증은 macOS 사용자.

## 금지 사항

- 카드 안 색/간격/아이콘 디테일을 결정하지 마라. 이유: 사용자가 UI 스킬로. 본 step 은 정보 구조와 동작.
- CertInfoSheet 안에 회차 미리보기·후기·과목 트리 추가 금지. 이유: docs/MOBILE_UX_SPEC.md 의 간략 패턴.
- `MainTabView.swift` 의 fullScreenCover 임시 진입 (step 5 에서 제거) 을 본 step 에서 복구하지 마라. 이유: 정문은 탭이다.
- DashboardView.swift 의 출석체크 패널·통계 그리드를 HomeView 로 옮기지 마라. 이유: 통계는 내정보 KPI 2x2 가 담당(step 7).

## Status 규칙

- macOS 빌드 통과: `completed`.
- 코드 작성만(Windows): `blocked`.
- 실패: 3회 후 `error`.
