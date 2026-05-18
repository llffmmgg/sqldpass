# Step 3 — Android 홈 정보 구조 재배치

## 배경

현재 HomeScreen 의 위→아래: 인사말 + Streak Mini Card + "오늘 바로 풀기" AccentCard (10문제 풀기/오프라인 준비) + "PASS+ 모의고사" ActionCard + Status.

신규 위→아래 (MOBILE_UX_SPEC.md 의 홈 정보 위계):
1. 인사말 헤더 (HeroHeader 유지)
2. **연속 학습 스트릭 카드** — 위험 톤 분기 (오늘 미풀이 시 warning)
3. **이어풀기 추천 카드** — 마지막 풀이 mode 분기로 진입
4. **자격증 6종 수평 캐러셀** — LazyRow + 카드 탭 시 자격증 소개 모달 시트

기존 "오늘 바로 풀기" 액션 카드와 "PASS+ 모의고사" 액션 카드는 제거. 풀이 정문은 실전 문제 탭, PASS+ 진입은 내정보 메뉴.

## 작업 디렉터리

`mobile/app/src/main/java/com/sqldpass/app/`

## 변경 대상

| 파일 | 변경 |
|---|---|
| `ui/home/HomeScreen.kt` | 본문 재구성 — 3 메인 카드 + 자격증 캐러셀. 기존 ActionCard / StatusCard 호출 제거. StreakMiniCard 를 위험 톤 분기로 확장(오늘 미풀이 + lastSolvedDate 가 어제 이전이면 warning). 새 컴포넌트 호출. |
| `ui/home/components/ContinueLastCard.kt` (신규) | 이어풀기 추천 — props: lastCert, lastMode (PRACTICE/MOCK_EXAM/PAST_EXAM/null), onClick. mode 별 텍스트 분기 ("어제 풀던 SQLD 랜덤 10문 이어가기" / "어제 시작한 모의고사 N회"). null 이면 카드 미노출. |
| `ui/home/components/CertCarousel.kt` (신규) | 자격증 6종 LazyRow 카드. 각 카드: 자격증 라벨·짧은 설명·색 dot. onClick 시 onCertCardTap(certKey) 호출. |
| `ui/home/components/CertInfoSheet.kt` (신규) | ModalBottomSheet — 시험 정보 4종(시행처/문항수/시간/합격기준) + "PASS+ 소개" CTA 1개. CTA 탭 시 navigate(PassPlus). 자격증 정보는 `ui/home/CertCatalog.kt` 신규 const 참조. |
| `ui/home/CertCatalog.kt` (신규) | 자격증 6종 정적 데이터 — 시행처/문항수/시간/합격기준. `frontend/src/lib/cert-tokens.ts` 와 정합. |
| `ui/AppViewModel.kt` | `lastSolveMode: String?` 추가 (PRACTICE/MOCK_EXAM/PAST_EXAM). `loadLastCert()` 가 mode 도 추정. 또는 별도 SharedPreferences 저장 — 본 step 은 SharedPreferences 패턴으로 간단히. |
| `MainActivity.kt` | HomeScreen 호출부의 props 갱신 — onContinue, onCertTap, onPurchase 콜백 전달. |

## Acceptance Criteria

1. `cd mobile; .\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL.
2. 홈 탭 진입 시 위→아래: 인사말 / 스트릭 / 이어풀기(있을 때만) / 자격증 캐러셀.
3. 스트릭 카드: 오늘 풀이 안 했고 lastSolvedDate 가 어제이면 warning 톤 ("오늘 자정이 끝나면 연속 일수가 끊겨요"). 그 외 normal 톤.
4. 이어풀기 카드 탭 → lastMode 가 MOCK_EXAM 이면 모의고사 탭 push + 마지막 회차 선택, PRACTICE 면 실전 문제 탭 push + 마지막 자격증 칩 선택, PAST_EXAM 이면 기출복원 탭 push.
5. 자격증 카드 탭 → CertInfoSheet 펼침. 시험 정보 4종 + PASS+ CTA 표시.
6. 기존 "10문제 풀기" / "PASS+ 모의고사" CTA 자취 없음.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

수동 시나리오:
- 홈 진입 → 4섹션 노출.
- 자격증 카드 탭 → 시트 펼침 → CTA 탭 → PassPlus 진입.
- 모의고사 1회 응시 후 홈 복귀 → 이어풀기 카드가 "모의고사" mode 로 표시.

## 금지 사항

- 카드 색상·간격·아이콘 디테일을 결정하지 마라. 이유: 사용자가 별 스킬로 UI 폴리시. 본 step 은 정보 구조만 — Material 기본값으로 충분.
- backdrop-blur, glow, opacity-only 옅은 배경 사용 금지. 이유: MEMORY 의 feedback_no_ai_blur_effects.
- 색 계열 변경 금지 (amber/emerald 그대로). 이유: MEMORY 의 feedback_color_token_changes.
- CertInfoSheet 안에 회차 미리보기·후기·과목 트리 등 추가 콘텐츠를 넣지 마라. 이유: 시트는 간략 패턴 — 시험 정보 4종 + CTA 1개로 제한.

## Status 규칙

- 성공: step 3 `completed`.
- 실패: 3회 후 `error`.
