# Step 2 — Android 변경 직접 책임의 lint warning 4건 정리

## 배경

Harness Review에서 발견: Android 전환 commit(`a366259..c5122ad`) 범위에서 새로 추가된 frontend 파일이 `npm run lint`에서 warning 4건을 만든다. errors는 아니라 build/CI를 깨지 않지만 누적되면 신호 노이즈가 커짐.

```
frontend/public/sw.js
  22:35  warning  'event' is defined but never used    @typescript-eslint/no-unused-vars
  82:12  warning  'e' is defined but never used        @typescript-eslint/no-unused-vars
  95:12  warning  'e' is defined but never used        @typescript-eslint/no-unused-vars

frontend/src/lib/offlineStore.ts
  219:23  warning  '_ignored' is assigned a value but never used  @typescript-eslint/no-unused-vars
```

`offlineStore.ts:219`는 이미 `_` prefix가 붙어 있는데도 룰이 잡는다 — ESLint의 `argsIgnorePattern`/`varsIgnorePattern`이 설정 안 됐거나, `_ignored`가 catch 변수가 아닌 일반 const로 선언돼 룰이 적용된 케이스.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

- `frontend/public/sw.js`
- `frontend/src/lib/offlineStore.ts`

## 변경 내용

### 1) `frontend/public/sw.js` — unused 콜백 인자 제거 또는 prefix

해당 라인의 콜백 시그니처를 확인하고:

- 인자가 진짜 안 쓰이면 제거 (`addEventListener("install", () => {...})` 같은 형태)
- API 시그니처상 두어야 한다면 `_event`, `_e`로 underscore prefix
- ESLint 룰을 disable comment로 끄지 마라. 이유: 다른 unused 인자까지 묻혀 들어갈 수 있음

`sw.js`는 Service Worker 표준 API라 `event` 객체를 콜백에서 받는 자리가 많음. 사용하지 않는 자리는 빈 인자 또는 `_event` 둘 중 코드 base의 다른 SW 콜백 패턴과 통일.

### 2) `frontend/src/lib/offlineStore.ts:219` — `_ignored` 처리

세 가지 옵션 중 하나 선택:

- **A (권장)**: `_ignored = await ...` → `await ...;` 결과 자체를 받지 않음 (의미상 fire-and-forget이라면)
- **B**: 값을 의도적으로 사용 — `void _ignored;` 한 줄 추가
- **C**: ESLint 설정 보강 — 프로젝트 ESLint 설정에 `argsIgnorePattern: '^_'` + `varsIgnorePattern: '^_'`를 추가해 `_` prefix 변수 일괄 면제. 이쪽이 근본 해결이지만 *별도 phase의 영역*이고 본 step에서는 손대지 마라.

→ **A를 우선 시도**. 코드 의도가 "결과를 무시"라면 `await`만 남기는 게 가장 깔끔.

## Acceptance Criteria

1. `frontend/`에서 `npm run lint`를 실행했을 때, 위 4건이 모두 사라진다.
2. lint output에 *새로운* warning/error가 추가되지 않는다 (회귀 없음).
3. `npm run build` 통과 (회귀 없음 보강).
4. SW 동작 변화 없음 — 이전과 동일한 cache 정책·라이프사이클.

## 금지 사항

- 다른 lint warning/error는 손대지 마라. 이유: 이번 step 범위는 Android 변경 책임의 4건뿐. 기존 코드 부채(13 errors)는 별도 phase `fix-react19-set-state-in-effect`에서 처리.
- ESLint config 자체를 수정하지 마라. 이유: 위 옵션 C는 광범위 영향. 별도 phase에서 다룬다.
- `// eslint-disable-next-line ...` 주석을 추가하지 마라. 이유: 코드 의도를 가린다. 인자 제거나 underscore prefix가 정공.

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

성공 조건:
- lint output에서 `sw.js`·`offlineStore.ts` warning이 모두 제거됨
- 13 errors는 그대로 남아 있어도 OK (별도 phase 영역)
- build 통과

## Status 규칙

- 성공: `phases/fix-android-minor/index.json`의 step 2 status를 `completed`로, summary에 "sw.js 3건 + offlineStore.ts 1건 unused warning 제거, lint 회귀 없음" 한 줄 기록.
- 실패: 3회 시도 후에도 lint 회귀가 발생하거나 SW 동작이 깨지면 `error` + `error_message` 기록.
- blocked: ESLint config 변경이 필요해 보이면 `blocked` + `blocked_reason: "argsIgnorePattern 설정 변경이 필요한지 사용자 확인 필요 — 별도 phase 영역과 겹침"` 기록.
