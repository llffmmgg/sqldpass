# Step 5 — 프로필 탭 (ProfileTab)

## 배경

iOS `ProfileView` 와 일관성 확보. 계정·구독·설정·지원을 한 탭으로. Dashboard 에 섞여 있던 닉네임 편집/테마 토글/로그아웃을 이관.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/ui/profile`

## 변경 대상

- `ui/profile/ProfileTab.kt` 신설.
- `ui/dashboard/NicknameEditDialog.kt` — 그대로 재사용 (이미 분리되어 있음).
- `MainActivity.kt` — Profile 라우트 호출부 추가.
- `ui/dashboard/DashboardTab.kt` — 닉네임 편집/테마 토글/로그아웃 메뉴 row 삭제. (Dashboard 자체는 라우트로는 살아 있으나 BOTTOM_TABS 에 없음. 본 step 에서 정리해서 다른 곳에서 호출되더라도 깨지지 않게 함수 시그니처 유지.)
- `AppViewModel.kt` — 진입 시 `loadSubscription()` 트리거 보장.

## UX 구성

```
HeroHeader: "마이"
├─ 프로필 카드 (Card)
│   - 🐙 닉네임  |  편집 아이콘 → NicknameEditDialog
│   - "가입일 2025-08-12" (me.createdAt formatKstDate)
│   - 비로그인: "Google 로그인" 버튼
├─ 구독 상태 카드
│   - active: "PASS+ 활성 (만료 YYYY-MM-DD)"  [구독 관리]
│   - inactive: "무료 플랜"  [PASS+ 알아보기 →]
├─ 학습 섹션 (Text section title + MenuListRow x N)
│   - 북마크한 문제 → (TODO: 다음 phase) 토스트 "곧 출시"
│   - 풀이 기록 → (TODO) 토스트
│   - 오프라인 콘텐츠 다운로드 → viewModel.sync()
├─ 설정 섹션
│   - 화면 테마 (시스템/라이트/다크 칩) ← DashboardTab 에서 이관
│   - 닉네임 편집 → 다이얼로그
├─ 지원 섹션
│   - 피드백 보내기 → FeedbackDialog (간단 TextField + 전송, viewModel.submitFeedback)
│   - 공지사항 (TODO 토스트)
│   - 이용약관 → 외부 브라우저 (https://www.sqldpass.com/terms)
│   - 개인정보처리방침 → 외부 브라우저
└─ 위험 영역
    - 로그아웃 → onLogout
    - 계정 삭제 → (TODO) 토스트 "고객센터로 문의"
```

세부:

- `MenuListRow` 컴포넌트 그대로 사용.
- 외부 링크는 `Intent(Intent.ACTION_VIEW, uri)`.
- 피드백 다이얼로그: 글자 수 제한 1000, type 고정 "GENERAL", pageUrl="mobile://profile".
- 비로그인 상태에서는 프로필 카드의 "Google 로그인" 버튼만 활성, 나머지 섹션은 로그인 안내 텍스트로 대체.

## Acceptance Criteria

1. `.\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL.
2. 비로그인 진입 → 로그인 CTA 만 보임.
3. 로그인 진입 → 프로필 카드/구독 상태/설정/지원 섹션 모두 표시, 테마 토글 동작, 닉네임 편집 동작.
4. Dashboard 라우트 호출 시 닉네임/테마/로그아웃 row 가 사라진 채로 그래프 카드만 표시 → 컴파일 통과.

## 금지 사항

- 신규 백엔드 endpoint 추가 금지. 이유: 본 step 은 클라이언트 UI 만.
- 닉네임 편집 다이얼로그 신규 작성 금지. 이유: 이미 `NicknameEditDialog.kt` 존재 — 재사용.
- 색 계열 변경 금지(메모리). 토큰만 사용.
- `backdrop-blur`, drop-shadow glow, opacity pulse 등 AI 효과 금지(메모리).

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```
