# Step 3 — 빌드 & 시뮬레이터/실기기 검증 (macOS 수동)

## 작업 디렉터리

```
ios/ (macOS 셸에서)
```

## 절차

```bash
cd ios
xcodebuild -project Sqldpass.xcodeproj -scheme Sqldpass \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -configuration Debug build
```

(xcodegen regenerate 불필요 — 새 파일 없음, 기존 파일 수정만)

## 체크포인트

1. **풀이 화면 진입 시 CustomTabBar 사라짐** — Home/MockExamsList/PastExamsList/SoloHub 에서 풀이 push 한 직후 화면 하단에 5탭 없음.
2. **정답/다음 버튼 완전히 보임** — ActionBar 가 잘림 없이 home indicator 영역까지 자연 stack.
3. **풀이 종료(pop/dismiss) 시 탭바 복원** — 5탭 다시 정상.
4. **탭바 등장/사라짐 시각 튐 없음** — Transaction.disablesAnimations 효과.
5. **다른 화면 회귀 0** — 홈/모의/기출/실전(허브)/내정보 모두 5탭 정상 + 직전 phase 의 흰색 통일 유지.
6. **다크 모드 회귀 0**.
7. **실기기에서 사용자 직접 확인** — 본 회귀의 원래 보고가 실기기였음.

## Acceptance Criteria

1. `xcodebuild ... build` `** BUILD SUCCEEDED **`.
2. 7 체크포인트 모두 만족.

## Status 규칙

- 성공: `completed` + summary "macOS 빌드 OK + 풀이 진입/이탈 탭바 hide/show + 정답 버튼 완전 노출 + 다른 화면 회귀 0".
- 실패: 사용자 보고에 따라 Step 1~2 중 해당 step `error` + 신규 fix step.
- 사용자 개입 필요 시: `blocked` + `blocked_reason`.
