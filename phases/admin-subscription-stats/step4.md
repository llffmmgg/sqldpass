# Step 4 — 프론트엔드: /admin/subscriptions "삭제" 액션

## 배경

Step 1 의 `DELETE /api/admin/subscriptions/{id}/archive` 를 어드민 화면에서 호출할 수 있게 한다. 현재 액션은 활성 구독에 "만료" 버튼만 있고, 만료된 구독에는 아무 액션이 없어 admin 테스트 결제 정리를 SQL 로 해야 함.

만료된 구독 행에 빨간 "삭제" 버튼 노출. 클릭 → prompt 사유 + confirm → archive 호출 → 목록 reload.

## 의존성

- Step 1 (`backend-subscription-archive`) 완료 필수.

## 작업 디렉터리

```
frontend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `src/lib/adminApi.ts` | `archiveSubscription(id: number, reason: string)` 함수 추가 — `DELETE /admin/subscriptions/{id}/archive?reason=...` |
| `src/app/admin/subscriptions/page.tsx` | `SubscriptionRow` 액션 분기: `sub.active` 면 기존 "만료" 버튼, 아니면 "삭제" 빨간 버튼. `handleArchive` 함수 추가. import 에 `archiveSubscription` 추가. |

## adminApi.ts 추가

기존 `expireSubscription` 옆에 동일 패턴으로:

```ts
export function archiveSubscription(id: number, reason: string) {
  const params = new URLSearchParams({ reason });
  return adminFetch<void>(`/subscriptions/${id}/archive?${params}`, {
    method: "DELETE",
  });
}
```

## SubscriptionRow 변경 (page.tsx L141-198)

`handleExpire` 패턴 그대로 복제한 `handleArchive`:

```tsx
async function handleArchive() {
  const reason = prompt(
    `구독 #${sub.id} (${sub.nickname}) 을 삭제합니다.\n통계 집계에서 빠지며 복구는 SQL 필요.\n사유를 입력하세요:`
  );
  if (!reason || !reason.trim()) return;
  if (!confirm("정말 삭제하시겠습니까? 통계에서 제외됩니다 (row 자체는 남음).")) return;

  setBusy(true);
  try {
    await archiveSubscription(sub.id, reason.trim());
    onChanged();
  } catch (e) {
    alert(e instanceof Error ? e.message : "삭제 처리 실패");
  } finally {
    setBusy(false);
  }
}
```

액션 셀 분기 (기존 L185-195):

```tsx
<DataTable.Cell align="right">
  {sub.active ? (
    <button
      onClick={handleExpire}
      disabled={busy}
      className="rounded border border-rose-500/40 bg-rose-500/10 px-2.5 py-1 text-[11px] font-medium text-rose-300 transition hover:bg-rose-500/20 disabled:opacity-50"
    >
      만료
    </button>
  ) : (
    <button
      onClick={handleArchive}
      disabled={busy}
      className="rounded border border-rose-500/40 bg-transparent px-2.5 py-1 text-[11px] font-medium text-rose-300 transition hover:bg-rose-500/10 disabled:opacity-50"
      title="통계 집계에서 제외 (테스트 결제 정리용)"
    >
      삭제
    </button>
  )}
</DataTable.Cell>
```

빨간 톤은 기존 rose-500/40 보더 그대로(메모리: 색 계열 변경 금지). 만료 vs 삭제는 배경 채움(만료 = bg-rose-500/10) vs 비움(삭제 = bg-transparent) 으로 구분 — 더 destructive 라는 시각 신호이지만 단단한 톤은 유지(글로우/펄스 금지).

## 검증

```powershell
cd frontend
npm run lint
npm run build
```

수동(dev 서버):
1. `/admin/login` → `/admin/subscriptions` 진입.
2. 만료된 구독 행에서 빨간 외곽 "삭제" 버튼 보임. 활성 구독에는 그대로 "만료".
3. "삭제" 클릭 → prompt → confirm → 200 응답 → 목록에서 사라짐(archived row 는 list 에서 제외, Step 1).
4. 사유 빈 문자열 → 동작 안 함(early return).
5. confirm 취소 → 동작 안 함.
6. 활성 구독에 archive 강제 호출 시도 시 백엔드가 400 응답 → alert 노출.

## Acceptance Criteria

1. `archiveSubscription(id, reason)` 함수 추가, `adminApi.ts` 타입 export 정합.
2. 활성/만료 분기로 액션 버튼 다름. 만료된 구독에만 "삭제" 노출.
3. prompt + confirm 가드 동작, 빈 사유 차단.
4. 성공 후 `onChanged()` 호출 → 목록 reload.
5. busy 상태에서 버튼 disabled.
6. `npm run lint` 0 error, `npm run build` 성공.

## 금지 사항

- 활성 구독에 "삭제" 버튼 노출하지 마라. **이유**: 백엔드가 거부하지만 UX 혼선. 활성은 먼저 만료해야 한다는 흐름을 시각적으로 강제.
- "삭제" 를 native delete 처럼 강한 경고로 만들지 마라. **이유**: 실제로는 soft-delete(통계 제외) — 경고 톤은 차분히.
- expireSubscription 기존 시그니처/엔드포인트 바꾸지 마라. **이유**: 만료와 삭제는 서로 다른 흐름, 동일 함수 재사용 금지.
- 새 모달을 만들지 마라 — 기존 prompt + confirm 패턴 유지. **이유**: 어드민 페이지 일관성, 모달은 grant 만.
- 색 계열을 다른 톤(amber/blue/violet 등)으로 바꾸지 마라. **이유**: 메모리 규칙 — 색 계열 변경 금지. rose 유지.

## Status 규칙

- 성공: step 4 `completed`, summary "adminApi.archiveSubscription + SubscriptionRow 활성/만료 분기(만료 행에 삭제 버튼) + prompt/confirm 가드, lint/build OK".
- 실패: 3회 재시도 후 `error`.
