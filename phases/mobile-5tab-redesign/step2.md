# Step 2 — MockExam 안에 기출복원 세그먼트

## 배경

PastExam 이 별도 탭이었으나 회차 카탈로그 성격이 같아 사용자 동선이 분리될 이유가 약함. MockExamTab 안에 [모의고사 ┃ 기출복원] 세그먼트 토글을 두고, 동일 카드 패턴으로 둘 다 렌더한다.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/ui/mockexam`

## 변경 대상

- `ui/mockexam/MockExamTab.kt` — 상단에:
  - **자격증 칩 드롭다운** (현재 `state.selectedCertSlug`/`selectCertSlug` 재사용)
  - **세그먼트**: `[모의고사 | 기출복원]` (Compose `SegmentedButton`)
- 모의고사 모드일 때: 기존 `state.mockExams` 카드 리스트.
- 기출복원 모드일 때: 기존 PastExamTab 의 회차 카드 리스트 (`state.pastExamsByCert[selectedCertSlug]` 사용).
- 자격증 선택 시 `viewModel.selectCertSlug(slug)` 로 PastExam 데이터 lazy 로드. 모의고사는 기존대로 전체 표시.
- `MainActivity.kt` — MockExam 라우트가 `onStartPastExam` 콜백도 받도록 `MockExamTab` 호출부 확장.
- `nav/SqldpassNav.kt` — `PastExam` route 는 deprecated 표기(주석 한 줄), BOTTOM_TABS 에서 제거된 상태 유지. 외부 NavHost 호출 경로 보존을 위해 라우트 자체는 남겨도 무방하나 사용처가 없으면 제거 가능. **이 step 에서는 라우트는 남기되 BOTTOM_TABS 에서 제거 상태**.

## UX 디테일

- 세그먼트는 **상단 고정**(스크롤시 함께 스크롤 OK), 자격증 드롭다운은 그 아래.
- 카드 좌측 액센트 색은 자격증 토큰(`LocalSqldpassSemanticColors.current.cert`) 매칭.
- 기출복원 카드의 부제는 `examYear ?? "—"` · `examRound ?? "—"`회.
- 비어 있는 자격증은 안내 카드("이 자격증의 기출이 아직 없습니다.").

## Acceptance Criteria

1. `.\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL.
2. 모의고사 탭 진입 → 자격증 토글 + 세그먼트 노출.
3. 기출복원 세그먼트 선택 → 자격증별 회차 카드 표시 → 카드 탭 시 Runner 진입(기존 `startPastExamRunner` 재사용).
4. 모의고사 세그먼트 → 기존 모의고사 카드 + 잠금 카드 동작 유지.

## 금지 사항

- AppViewModel 시그니처 변경 금지. 이유: 본 step 은 UI 통합. 기존 `selectCertSlug`/`loadPastExams`/`startPastExamRunner` 그대로 사용.
- PastExamTab.kt 파일 삭제 금지. 이유: 후속 정리 step 까지는 fallback 으로 둠. 다만 BOTTOM_TABS 에서 빠져 있으므로 사용자 접근 없음.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```
