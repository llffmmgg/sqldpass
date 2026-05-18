# Step 4 — Android 내정보 KPI 그리드 + 3그룹 메뉴

## 배경

ProfileTab 이 사용자의 통계 요약 + 학습 흐름 진입 허브 + 계정 설정 모두를 담는 책임 부담 탭이 됨 (Insights·WrongAnswers 탭 흡수).

신규 위→아래:
1. 프로필 헤더 (닉네임 · 구독 상태 배지)
2. **KPI 2x2 아이콘 그리드** — 총 풀이 / 평균 정답률 / 최장 스트릭 / 합격 확률
3. **학습 메뉴**: 오답노트 / 북마크 / 풀이 이력
4. **계정 메뉴**: 닉네임 편집 / 결제·청구 / PASS+ 구독 관리
5. **설정·지원 메뉴**: 테마 / 알림 / 피드백 / 공지·약관 / 로그아웃 / 회원 탈퇴

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/`

## 변경 대상

| 파일 | 변경 |
|---|---|
| `ui/profile/ProfileTab.kt` | 본문 재구성 — 프로필 헤더 + KPI 2x2 + 3섹션 메뉴. 기존 학습/설정/지원 섹션 정리해 본 plan 의 3그룹 정합. |
| `ui/profile/components/KpiGrid.kt` (신규) | 2x2 LazyVerticalGrid 또는 단순 2 Row. 4개 KPI 아이콘 + 라벨 + 값. 값은 viewModel 의 4개 필드 참조. 데이터 없으면 "—" 표시. |
| `ui/AppViewModel.kt` | `profileKpi: ProfileKpi` 필드 추가. `loadProfileKpi()` 메서드 — streak.longestStreak 직접 사용, totalSolved/avgRate/passProbability 는 본 step 에서 placeholder(`null`)로 두고 별 phase 의 백엔드 신설 대기. 단 `frontend/src/lib/dashboard/passRate.ts` 의 계산식을 Kotlin 으로 포팅 가능하면 시도 — `wrongAnswerStats` + `streak` 데이터로 계산. |
| `ui/profile/ProfileTab.kt` 의 학습 메뉴 행 | 오답노트 / 북마크 / 풀이 이력 행 신규 추가. 탭 시 각각 navigate(WrongAnswers.route) / navigate(BookmarksScreen.route — 신규) / navigate(HistoryScreen.route — 신규 또는 기존 라우트 확인). 본 step 은 라우트 진입만 — 화면 본체 변경 없음. |
| `nav/SqldpassNav.kt` | `BookmarksScreen`, `HistoryScreen` 라우트가 없으면 추가. 화면 본체는 기존 컴포넌트 재사용 또는 placeholder. |

## ProfileKpi data class (`ui/AppViewModel.kt`)

```kotlin
data class ProfileKpi(
    val totalSolved: Int? = null,        // 누적 풀이 수 — 별 phase 의 백엔드 API 대기
    val avgCorrectRate: Int? = null,     // 평균 정답률 (%) — 별 phase 대기
    val longestStreak: Int? = null,      // /api/streak/me 의 longestStreak
    val passProbability: Int? = null,    // frontend passRate.ts 포팅 또는 별 phase 대기
)
```

## Acceptance Criteria

1. `cd mobile; .\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL.
2. 내정보 탭 진입 시 위→아래: 헤더 / KPI 2x2 / 학습 / 계정 / 설정·지원.
3. KPI 4개 중 longestStreak 는 실제 값 (있을 때). 나머지 3개는 "—" 또는 placeholder.
4. 학습 섹션의 "오답노트" 행 탭 → 기존 WrongAnswerTab 화면이 풀스크린 라우트로 push.
5. 계정 섹션의 PASS+ 행 탭 → PassPlus 라우트.
6. 메뉴 행 순서는 위 spec 정확히 일치.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

수동 시나리오:
- 내정보 탭 → KPI 표시.
- 오답노트 행 탭 → WrongAnswerTab 화면 push.
- 닉네임 행 탭 → 닉네임 편집 다이얼로그.

## 금지 사항

- KPI 의 누적 풀이·평균 정답률·합격 확률을 본 step 에서 백엔드 신설로 채우지 마라. 이유: 별 phase 의 `kpi-backend-support` 작업. 본 step 은 placeholder 또는 클라이언트 reuse 만.
- WrongAnswerTab.kt / InsightsTab.kt 화면 본체를 손대지 마라. 이유: 본 step 은 진입 라우팅만.
- "이력" 메뉴 행이 향하는 화면이 없으면 placeholder 라우트 + EmptyState 만 만들고 끝. 기존 코드 다양한 곳에 흩어진 풀이 이력 컴포넌트 통합은 별 phase.
- 메뉴 그룹 순서를 바꾸지 마라. 이유: docs/MOBILE_UX_SPEC.md 의 양 플랫폼 일치 약속.

## Status 규칙

- 성공: step 4 `completed`.
- 실패: 3회 후 `error`.
