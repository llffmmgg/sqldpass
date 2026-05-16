# Step 1 — Backend: 블로그 PDF 다운로드 컨트롤러 (Thunder 이상 게이팅)

## 배경

ADsP 시험 D-1 응원 블로그 글에서 Thunder 이상 회원에게만 PDF 정리본을 받을 수 있게 한다. 정적 PDF 를 `frontend/public/` 에 두면 누구나 URL 추측으로 접근 가능하므로, 백엔드가 권한 검증 후 바이트 스트리밍한다.

`MockExamController#downloadPdf` (`/api/mock-exams/{id}/pdf/download`) 의 패턴을 그대로 차용한다 — `subscriptionService.allowsPdf(memberId)` 검증 후 `Content-Disposition: attachment; filename*=UTF-8''…` 헤더로 응답.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `backend/src/main/resources/blog-downloads/adsp-cram.pdf` | 사용자가 직접 데스크탑에서 복사하여 배치 (이 step 에서 만들지 않음) |
| `backend/src/main/java/com/sqldpass/controller/blog/BlogDownloadController.java` | 신규 — `GET /api/blog-downloads/adsp-cram` |
| `backend/src/main/java/com/sqldpass/config/WebMvcConfig.java` | `memberAuthInterceptor.addPathPatterns(...)` 에 `/api/blog-downloads/**` 추가 |

## 컨트롤러 구현 규격

- `@RestController @RequestMapping("/api/blog-downloads")`
- 의존: `SubscriptionService` (생성자 주입 — Lombok `@RequiredArgsConstructor`)
- 엔드포인트: `GET /adsp-cram`
- 흐름:
  1. `memberId = (Long) request.getAttribute("memberId")`
  2. memberId null → `throw new SqldpassException(ErrorCode.UNAUTHORIZED)`
  3. `subscriptionService.allowsPdf(memberId)` false → `throw new SqldpassException(ErrorCode.PDF_REQUIRES_SUBSCRIPTION)`
  4. `new ClassPathResource("blog-downloads/adsp-cram.pdf")` → `Files.readAllBytes(resource.getFile().toPath())` 대신 `resource.getInputStream()` 으로 `byte[]` 읽기 (JAR 안에서도 동작하도록 `StreamUtils.copyToByteArray`)
  5. 응답:
     - `Content-Type: application/pdf`
     - `Content-Disposition: attachment; filename*=UTF-8''` + URLEncoder 로 인코딩된 `adsp_문어_벼락치기.pdf`
     - body: bytes
- 리소스 없으면 (`!resource.exists()` 또는 IOException) → 500 으로 자연 폴백 — 운영에서는 PDF 가 항상 있어야 정상.

## WebMvcConfig 변경

`memberAuthInterceptor.addPathPatterns(...)` 목록 (현재 L31~L43) 끝에 `"/api/blog-downloads/**"` 한 줄 추가.

## 재사용 (신규 코드 최소화)

- `service/common/ErrorCode.java` 의 `UNAUTHORIZED`, `PDF_REQUIRES_SUBSCRIPTION` — 신규 코드 없음
- `service/payment/SubscriptionService#allowsPdf(memberId)` — 그대로 호출
- `MockExamController` L128–142 의 응답 헤더 구성 패턴 (URLEncoder + filename\*=UTF-8'')

## 검증

```powershell
cd backend
.\gradlew.bat compileJava
```

수동: PDF 가 배치된 상태에서 `bootRun` → `curl -i http://localhost:8080/api/blog-downloads/adsp-cram`
- 비로그인: 401 + `UNAUTHORIZED`
- Free 회원: 403 + `PDF_REQUIRES_SUBSCRIPTION`
- Thunder 회원: 200 + 바이트, `Content-Disposition` 에 한글 파일명

## Acceptance Criteria

1. `BlogDownloadController` 가 신규로 추가되었고, `subscriptionService.allowsPdf` 검증을 거친다.
2. `WebMvcConfig` 의 `memberAuthInterceptor` 에 `/api/blog-downloads/**` 가 등록되어 있다.
3. `.\gradlew.bat compileJava` 통과.

## 금지 사항

- PDF 본체를 `frontend/public/` 에 두지 마라. **이유**: 누구나 URL 직타로 접근 가능해져 권한 게이팅이 무의미해진다.
- `MockExamController` 또는 기존 PDF 흐름을 수정하지 마라. **이유**: 본 작업은 신규 정적 PDF 한 건. 모의고사 PDF 동작에 영향 주면 안 된다.
- 새 ErrorCode 를 만들지 마라. **이유**: `UNAUTHORIZED` / `PDF_REQUIRES_SUBSCRIPTION` 그대로 재사용해 프론트 분기 로직(toast 코드 매칭) 일관성을 유지.
- Flyway 마이그레이션을 추가하지 마라. **이유**: 본 작업은 DB 스키마 변경 없음.

## Status 규칙

- 성공: step 1 `completed`, summary 에 "BlogDownloadController + WebMvcConfig 등록, compileJava OK".
- 실패: 3회 재시도 후 `error`.
