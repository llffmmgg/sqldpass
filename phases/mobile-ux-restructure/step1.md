# Step 1 — Mobile UX 단일 진실 원천 문서 (`docs/MOBILE_UX_SPEC.md`)

## 배경

Plan `C:\Users\admin\.claude\plans\giggly-brewing-crystal.md` 의 17개 UX 결정을 양 플랫폼 구현이 참조할 정식 문서로 옮긴다. 본 phase 의 step 2~7 이 본 문서를 absolute spec 으로 참조 — 각 step 의 변경은 본 문서와 1:1 매핑되어야 한다.

`docs/SOLVE_SCREEN_SPEC.md` 와 동등 위상.

## 작업 디렉터리

`docs/`

## 변경 대상

| 파일 | 변경 |
|---|---|
| `docs/MOBILE_UX_SPEC.md` | 신규 — plan 의 핵심 결정 압축본 |

## 본문 구성 (필수 섹션)

1. **탭 구성** (5탭 좌→우: 홈 / 모의고사 / 기출복원 / 실전 문제 / 내정보)
2. **각 탭 정보 위계** (위→아래 bullet)
3. **핵심 동선 표** (사용자 의도 → 탭 수 → 경로)
4. **양 플랫폼 동일 규칙 14가지** (탭 순서·이름 / 모달 시트 / 풀이 정문 / 빈 상태 / 모드 라벨 / 스트릭 위험 인앱톤 / 검색 정책 / 자격증 시트 / SessionComplete 풍부도 등)
5. **자격증 6종 정적 데이터** (시행처·문항수·시간·합격기준 — 양 플랫폼이 동일 JSON 또는 const 참조)
6. **변경 이력 표**

## Acceptance Criteria

1. `docs/MOBILE_UX_SPEC.md` 가 신규로 존재.
2. plan 파일의 17개 결정이 본 문서에 빠짐 없이 포함 (Q1~Q17 일대일).
3. 자격증 6종 정적 데이터의 위치 (예: `frontend/src/lib/cert-tokens.ts` 또는 별도 JSON) 가 명시되어 양 플랫폼이 어디서 가져올지 분명.
4. 빌드 영향 없음 (코드 변경 없음, 순수 문서).

## 검증

본 step 은 문서만 — 별도 검증 명령 없음.

```powershell
# 문서 존재 확인
ls docs/MOBILE_UX_SPEC.md
```

## 금지 사항

- plan 파일의 결정과 다른 새 결정을 본 step 에서 도입하지 마라. 이유: plan 이 진실 원천이며 본 문서는 그 미러. 결정 변경은 plan 파일 갱신이 선행되어야 함.
- UI 디테일(색 hex·간격 dp·아이콘 이름)을 본 문서에 박지 마라. 이유: 사용자가 UI 는 별 스킬로 진행. 본 문서는 정보 구조·동작만.
- `docs/SOLVE_SCREEN_SPEC.md` 의 내용을 복제하지 마라. 이유: 풀이 화면 디테일은 그 문서가 진실 원천. 본 문서는 풀이 진입까지 + 그 외 탭/홈/내정보.

## Status 규칙

- 성공: index.json step 1 `completed`, summary 한 줄.
- 실패: 3회 후 `error`.
