# Step 1 — Android BookmarksScreen 신설

## 작업 디렉터리
`mobile/`

## 배경 / Why
- iOS `BookmarksView` + `BookmarkDetailSheet` 는 완전 구현됨. Android 는 `ProfileTab.kt:186-190` 에서 "북마크 화면은 곧 출시됩니다" 토스트만 표시 → 페어리티 갭 (P1-1).
- 백엔드 `/api/bookmarks` GET/POST/DELETE + `/api/bookmarks/exists/{id}` 는 이미 `SqldpassApi.kt:100-111` 에 wired.

## 변경 대상

### 1. ViewModel 상태 확장 (`mobile/app/src/main/java/com/sqldpass/app/ui/AppViewModel.kt`)
- `AppUiState` 에 다음 필드 추가:
  - `val bookmarks: List<BookmarkSummary> = emptyList()` — BookmarkListResponse.items 의 mirror
  - `val bookmarksLoading: Boolean = false`
  - `val bookmarksError: String? = null`
- 메서드 추가:
  - `fun loadBookmarks()` — 로그인 가드(token null 시 return) → `repository.api.getBookmarks()` 호출 → state update
  - 기존 `toggleBookmark(questionId)` 는 그대로 두되 본 화면이 사용하는 API: 북마크 해제 시 list 갱신
- 모델: `BookmarkSummary` 가 `data/ApiModels.kt` 의 `BookmarkListResponse` 안에 이미 있는지 확인. 없으면 추가.

### 2. Repository wiring (`mobile/app/src/main/java/com/sqldpass/app/data/AppRepository.kt`)
- 직접 호출 패턴: ViewModel 이 `repository.api.X` 보다는 `repository.loadBookmarks()` 같은 메서드 노출이 일관.
- 기존 패턴 따라 메서드 신설:
  ```kotlin
  suspend fun loadBookmarks(): BookmarkListResponse = api.getBookmarks()
  suspend fun removeBookmark(questionId: Long) { api.removeBookmark(questionId) }
  ```

### 3. 새 화면 (`mobile/app/src/main/java/com/sqldpass/app/ui/bookmarks/BookmarksScreen.kt`)
- iOS `BookmarksView.swift` 정보 구조와 동일:
  - 상단 TopAppBar: 뒤로가기 + "북마크" 제목
  - LazyColumn 으로 `bookmarks` 표시
  - 항목 카드: 문제 본문 일부 (snippet) + 자격증/주제 라벨 + 우측 북마크 토글
  - 토글 탭 → `viewModel.toggleBookmark(id)` → 낙관적 제거
  - 비어있으면 AppStateView empty 표시
  - 로딩 중 AppStateView loading
  - 에러 시 AppStateView error + 재시도
- 디자인 시스템 토큰 사용 — `AppCard`, `AppCardSurface.Card`, `LocalSqldpassPalette`, `AppSectionHeader` 등.
- `LaunchedEffect(Unit) { onLoadBookmarks() }` 로 첫 진입 시 fetch.

### 4. NavGraph 라우트 추가 (`mobile/app/src/main/java/com/sqldpass/app/nav/SqldpassNav.kt`)
- 기존 `SqldpassRoute` sealed class 에 `Bookmarks` 추가:
  ```kotlin
  object Bookmarks : SqldpassRoute("bookmarks")
  ```
- `MainActivity.kt` 의 NavHost 에 composable(SqldpassRoute.Bookmarks.route) 추가.
- 트랜지션은 다른 화면과 동일하게 `pushSlideEnter` / `pushSlideExitForward` 적용.

### 5. ProfileTab 갱신 (`mobile/app/src/main/java/com/sqldpass/app/ui/profile/ProfileTab.kt`)
- `pendingNotice = "북마크 화면은 곧 출시됩니다."` 부분(line ~186-189)을 `onClick = onOpenBookmarks` 로 변경.
- `ProfileTab` Composable signature 에 `onOpenBookmarks: () -> Unit` 파라미터 추가.
- 호출처(MainActivity) 에서 `onOpenBookmarks = { navController.navigate(SqldpassRoute.Bookmarks.route) }` 주입.

## 작업 절차
1. ApiModels.kt 의 `BookmarkListResponse` / `BookmarkSummary` 구조 확인.
2. AppUiState + ViewModel + Repository 메서드 추가.
3. BookmarksScreen.kt 신설 (디자인 시스템 토큰만 사용).
4. SqldpassRoute.Bookmarks 등록 + NavHost 추가.
5. ProfileTab + MainActivity 호출 경로 연결.
6. `:app:assembleDebug` 통과 확인.

## 검증
```powershell
cd C:\\Users\\admin\\desktop\\sqldpass\\sqldpass\\mobile
.\\gradlew.bat :app:assembleDebug
```
- BUILD SUCCESSFUL 출력 필수.

## 금지사항
- 신규 Material3 컴포넌트(`AlertDialog`, `Card`, `Button` 등) 직접 사용 금지. 이유: `android-polish-and-shared-renderer` phase 에서 모든 화면이 `App*` primitive 로 통일됨.
- `MaterialTheme.colorScheme.X` 직접 색 사용 금지. 이유: `LocalSqldpassPalette.current` 토큰만 사용.
- 백엔드 API 추가/변경 금지. 이유: 본 phase 는 Android 단독.
- 북마크 해제 UI 가 실제 `DELETE /api/bookmarks/{id}` 를 호출하지 않으면 안 됨 — 낙관적 UI 만 해놓고 실 호출 누락 금지.

## 산출물
- 신규/수정 파일 목록 + 핵심 로직 1-2줄 설명.
- `:app:assembleDebug` 결과 마지막 5줄.
