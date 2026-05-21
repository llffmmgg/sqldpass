# Step 4 — 빌드 & 시뮬레이터/실기기 검증 (macOS 수동)

## 작업

```bash
cd ios
~/bin/xcodegen generate   # UINavigationController+SwipeBack.swift 새 파일 인식
xcodebuild -project Sqldpass.xcodeproj -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build
```

## 체크포인트 (7 개)

1. **MockExamDetailView 진입 시 탭바 사라짐** — 모의고사 리스트 → 상세 push. 5탭 없음. "시험 시작하기" 잘림 0.
2. **상세에서 뒤로 가면 탭바 복원** — pop 후 5탭.
3. **앱 전체 swipe back 동작** — 모든 push 화면(MockExamDetail, Profile/닉네임 편집, History/상세, Paywall, SoloSubjects 등)에서 좌→우 swipe 로 뒤로 가기.
4. **풀이 화면 swipe 시 종료 confirm alert 트리거** — Solve/SoloSolve/PastExamRunner 에서 swipe 시작하면 "정말 종료할까요?" alert. "계속 풀기" → 닫고 풀이 유지, "종료" → dismiss → 답안 잃음(X 버튼과 동등 동작).
5. **swipe 시작 후 손 떼면 interactive 복귀** — iOS 표준 동작.
6. **루트(탭별) 화면 swipe 무효** — viewControllers.count > 1 가드.
7. **다크 모드 회귀 0**.

## Acceptance Criteria

1. xcodebuild ** BUILD SUCCEEDED **.
2. 7 체크포인트 모두 만족.

## Status 규칙

- 성공: `completed` + summary "macOS 빌드 OK + 7 체크포인트 만족".
- 실패: 사용자 보고에 따라 Step 1~3 중 해당 step `error` + 신규 fix step.
