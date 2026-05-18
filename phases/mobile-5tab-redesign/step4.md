# Step 4 — 인사이트 탭 (InsightsTab)

## 배경

Dashboard 의 시각화(과목별 정답률·일자별 풀이량·회차별 최고점)를 독립 탭으로 이관해 "데이터로 보는 약점" 진입을 명확히 한다.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/ui/insights`

## 변경 대상

- `ui/insights/InsightsTab.kt` 신설.
- `ui/dashboard/DailyChart.kt` — 그대로 재사용(이동 X).
- `MainActivity.kt` — Insights 라우트가 wrongAnswerStats/dashboard 데이터 콜백 받도록 연결.
- `AppViewModel.kt` — 진입 시 `loadDashboard()` + `loadWrongAnswerStats()` 트리거. 이미 함수는 존재.

## UX 구성

```
HeroHeader: "데이터로 보는 약점"
├─ 과목별 정답률 카드
│   - 정답률 = 100 - wrongRate
│   - 가로 막대: 우측 % 라벨, 70% 미만은 우측 ⚠️ 아이콘 + danger 색
│   - 100% 일 땐 success 색
├─ 일자별 풀이량 카드
│   - DailyChart(state.dashboard?.dailyCounts) 재사용
│   - 부제: "최근 14일"
├─ 회차별 최고 점수 카드 (collapsible 또는 max 5개)
│   - mockExamId → exam name 매핑 (state.mockExams)
│   - 점수 + 정답률 (%)
└─ 빈 상태: CtaCard("로그인 후 인사이트 확인")
```

세부:

- 막대 차트는 외부 라이브러리 도입 금지 — `Box` + `width = fraction` 컴포저로 충분.
- 정답률 = `100 - wrongRate` (wrongRate 이 백분율 정수임을 ApiModels 시그니처에서 확인: `wrongRate: Int`).
- 카드 토큰은 기존 `Card(shape=RoundedCornerShape(14.dp))` 패턴.
- 자격증/과목 색은 토큰만(`LocalSqldpassSemanticColors.current.cert.sqld` 등) 사용. 색 계열 변경 금지 메모리 준수.
- AI blur/glow 효과 금지(메모리). 단단한 톤 유지.

## Acceptance Criteria

1. `.\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL.
2. 비로그인: CtaCard.
3. 로그인: 3개 카드 모두 표시. wrongAnswerStats 가 비면 "취약 데이터 없음" 안내.
4. fontScale 1.5x preview 깨짐 없음.

## 금지 사항

- MPAndroidChart 등 차트 라이브러리 추가 금지. 이유: 막대/라인 차트는 인하우스 Box 컴포저로 충분 — 의존성 부풀리지 마라.
- WrongAnswerStats 의 막대 길이를 `wrongRate` 기준으로 시각화 금지. 이유: 사용자는 정답률(=잘하는 정도)을 더 직관적으로 인식 — 100-wrongRate 기준.
- Dashboard 의 streak/avg/best-score 카드 자체를 삭제 금지. 이유: 본 step 은 신규 화면 추가, Dashboard 정리는 다음 step.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```
