# Step 7 — iOS 내정보 KPI 그리드 + 3그룹 메뉴

## 배경

Android step 4 와 동등 위계. ProfileView 가 사용자 통계 요약 + 학습 흐름 진입 허브 + 계정 설정 모두를 담음.

신규 위→아래:
1. 프로필 헤더 (닉네임 · 구독 상태 배지)
2. KPI 2x2 LazyVGrid (총 풀이 / 평균 정답률 / 최장 스트릭 / 합격 확률)
3. List 안 3섹션:
   - 학습: 오답노트 / 북마크 / 풀이 이력
   - 계정: 닉네임 편집 / 결제·청구 / PASS+ 구독 관리
   - 설정·지원: 테마 / 알림 / 피드백 / 공지·약관 / 로그아웃 / 회원 탈퇴

macOS 검증 대기.

## 작업 디렉터리

`ios/`

## 변경 대상

| 파일 | 변경 |
|---|---|
| `Features/Profile/ProfileView.swift` | 본문 재배치. 기존 List/Form 구조에 KPI 그리드 상단 삽입 + 학습 섹션 NavigationLink 3개 (WrongAnswersView, BookmarksView, HistoryView) + 계정·설정 섹션 정렬. |
| `Features/Profile/Components/KpiGrid.swift` (신규) | LazyVGrid(columns: 2) 4개 KpiTile. 값 nil 이면 "—" 표시. |
| `Features/Profile/Components/KpiTile.swift` (신규) | 아이콘 + 라벨 + 값. |
| `Features/Profile/ProfileViewModel.swift` | `kpi: ProfileKpi` 필드 추가. `loadKpi()` 메서드 — longestStreak 만 실데이터, 나머지 3개 placeholder(`nil`). 합격 확률은 `wrongAnswerStats` + `streak` 으로 계산 가능하면 시도. |
| `Features/Profile/Models/ProfileKpi.swift` (신규) | struct ProfileKpi { totalSolved: Int?, avgCorrectRate: Int?, longestStreak: Int?, passProbability: Int? }. |
| `Features/WrongAnswers/WrongAnswersView.swift` | MainTabView 의 탭 등록(step 5)에서 빠짐 — ProfileView 의 NavigationLink 으로 진입. 화면 본체 변경 없음. |
| `Features/Bookmarks/BookmarksView.swift` | 동일. 신규 NavigationLink 진입처. |
| `Features/History/HistoryView.swift` | 동일. 신규 NavigationLink 진입처. |

## Acceptance Criteria (macOS 환경)

1. `xcodebuild ... build` → BUILD SUCCEEDED.
2. 내정보 탭 진입 시 위→아래: 헤더 / KPI 2x2 / 학습 섹션 / 계정 섹션 / 설정·지원 섹션.
3. KPI 중 최장 스트릭은 실제 값, 나머지는 "—".
4. 학습 섹션 "오답노트" 행 탭 → WrongAnswersView push.
5. 학습 섹션 "북마크" 행 탭 → BookmarksView push.
6. 계정 섹션 "PASS+ 구독" 행 탭 → PaywallView (또는 동등 화면).
7. 설정·지원 섹션의 로그아웃·탈퇴 행 동작은 기존 그대로.

## 검증

```bash
cd ios
~/bin/xcodegen generate
xcodebuild ... build
```

**Windows 환경에서 빌드 불가** — 코드만 작성, 검증은 macOS 사용자.

## 금지 사항

- KPI 4개 중 placeholder 인 3개를 본 step 에서 백엔드 신설로 채우지 마라. 이유: 별 phase `kpi-backend-support`. 본 step 은 placeholder 또는 클라이언트 계산만.
- WrongAnswersView / BookmarksView / HistoryView 본체를 손대지 마라. 이유: 본 step 은 진입 라우팅만 — 화면 변경은 별 phase.
- 메뉴 그룹 순서나 행 순서를 임의로 바꾸지 마라. 이유: docs/MOBILE_UX_SPEC.md 의 양 플랫폼 동일 약속.
- 설정·지원 섹션에 푸시 알림 권한 요청 UI 를 추가하지 마라. 이유: 본 plan 의 Q15 결정 — 푸시 알림 없음.

## Status 규칙

- macOS 빌드 통과: `completed`.
- 코드 작성만(Windows): `blocked`.
- 실패: 3회 후 `error`.
