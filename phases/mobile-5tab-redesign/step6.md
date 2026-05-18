# Step 6 — Home FAB + 검증

## 배경

Solve 탭이 BOTTOM_TABS 에서 빠지면서 "10문제 빠른풀이" 진입을 홈에 흡수해야 한다. Streak 요약을 홈 최상단에 노출해 회귀 동기 제공.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app`

## 변경 대상

- `ui/home/HomeScreen.kt` —
  - 최상단 추천 카드 아래 또는 위에 **Streak Mini Card**(`state.dashboard?.streak?.currentStreak` 표시, 0 일 땐 "오늘 시작해보세요").
  - "10문제 풀기" 버튼은 그대로 유지(기존 onQuickPractice 콜백).
  - **FAB** 는 본 step 에서는 생략. 이유: Home CTA 가 이미 충분. FAB 는 향후 별도 phase.
- `MainActivity.kt` —
  - Home 진입 시 `loadDashboard()` 트리거 (이미 Dashboard 에서만 호출 → Home 에서도 호출 보강).
  - `onQuickPractice` 콜백이 Solve 라우트로 navigate (기존 동작 유지).
- `nav/SqldpassNav.kt` — `Solve` route 는 그대로 enum 유지(BOTTOM_TABS 에서만 빠져 있음).
- `phases/mobile-5tab-redesign/index.json` — 모든 step 상태 `completed` 처리, `completed_at` 기록.
- `phases/index.json` — phase 항목에 `completed_at` 기록.

## Acceptance Criteria

1. `.\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL.
2. 하단 탭 5개: 홈/모의고사/오답노트/인사이트/마이.
3. Home 에서 "10문제 풀기" → Solve 화면 진입 → 풀이 → Runner → 결과 → 홈 복귀 동작.
4. fontScale 1.5x preview 깨짐 없음 (Home).

## 금지 사항

- 신규 라이브러리 추가 금지. 이유: 본 step 은 마무리 단계.
- BOTTOM_TABS 에 6번째 탭 추가 금지. 이유: Material 권장 5개 최대 + 사용자 합의된 정보 구조.
- Solve 라우트 enum 제거 금지. 이유: Home CTA 가 navigate 로 의존.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```
