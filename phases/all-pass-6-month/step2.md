# Step 2 — 모바일 PASS+ 카탈로그 텍스트 갱신

## 배경

Step 1 에서 백엔드 `UNLIMITED` plan 의 기간이 평생 → 6개월 로 변경되었으므로 모바일 PASS+ 카탈로그 화면(`PassPlusCatalogScreen.kt`) 의 사용자 노출 텍스트만 정합시킨다. `planLabel="All Pass"`, `productId="iap_unlimited"` 는 사용자 결정으로 그대로 유지.

## 작업 디렉터리

```
mobile/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `mobile/app/src/main/java/com/sqldpass/app/ui/passplus/PassPlusCatalogScreen.kt` (L69-75) | DEFAULT_CATALOG 의 All Pass 항목 durationLabel / benefits |

## 변경 diff

```diff
     CatalogEntry(
         productId = "iap_unlimited",
         planLabel = "All Pass",
-        durationLabel = "평생",
-        benefits = listOf("Pro 의 모든 혜택", "평생 무제한", "출시 후 추가 기능 포함"),
+        durationLabel = "6개월",
+        benefits = listOf("Pro 의 모든 혜택", "6개월 PASS+ 무제한 풀이", "모의고사 PDF 다운로드"),
     ),
```

세 번째 benefit 의 기존 문구 "출시 후 추가 기능 포함" 은 약속성이 모호하므로 같이 정리해서 PDF 다운로드 (UNLIMITED 만 가지는 차별점) 를 명시.

## 검증

```powershell
cd mobile
.\gradlew.bat :app:assembleDebug
```

UI 빌드 컴파일 통과면 OK. 실 시각 검수는 사용자가 에뮬레이터/실기기에서 PASS+ 화면 진입해 All Pass 카드의 기간이 "6개월" 로 보이는지 1회 확인 (선택).

## Acceptance Criteria

1. `PassPlusCatalogScreen.kt` 의 All Pass CatalogEntry 의 `durationLabel = "6개월"`, benefits 가 갱신됨
2. `:app:assembleDebug` 통과
3. 다른 3개 CatalogEntry(Thunder/Focus/Pro) 는 손대지 않음

## 금지 사항

- `productId = "iap_unlimited"` 를 변경하지 마라. **이유**: Play Console 등록된 SKU 와 일치해야 함. 변경 시 결제 실패.
- `planLabel = "All Pass"` 를 변경하지 마라. **이유**: 사용자 결정 — 라벨 유지.
- Thunder/Focus/Pro 의 durationLabel("3일"/"30일") 을 같이 손보지 마라. **이유**: 본 변경 범위 밖.

## Status 규칙

- 성공: step 2 `completed`, summary "PassPlusCatalogScreen.kt All Pass durationLabel/benefits 6개월 톤으로 갱신, debug 빌드 OK".
- 실패: 3회 재시도 후 `error`.
