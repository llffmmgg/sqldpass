---
name: security-review
description: Spring Boot 백엔드와 Next.js 프론트엔드의 보안 취약점을 점검한다. "보안 리뷰", "보안 점검", "취약점 확인", "SQL Injection 검사", "XSS 검사", "인증 확인", "인가 확인", "시크릿 누출 확인" 등의 요청이 있을 때 사용.
---

# 보안 리뷰 스킬 (Spring Boot + Next.js)

## 역할
Spring Boot 백엔드와 Next.js 프론트엔드 코드베이스의 보안 취약점을 분석한다.

## 리뷰 항목

### Backend (Spring Boot)
1. **SQL Injection**
   - JPA `@Query`에서 문자열 연결 사용 여부 → `:param` 바인딩 사용 권장
   - Native Query 사용 시 파라미터 바인딩 확인
   - `JdbcTemplate`에서 `?` 플레이스홀더 사용 여부

2. **인증/인가**
   - `SecurityFilterChain`에서 엔드포인트별 권한 설정 누락
   - JWT 토큰 검증 로직 (만료, 서명, 변조)
   - `@PreAuthorize`, `@Secured` 누락된 민감 API
   - IDOR (Insecure Direct Object Reference) — 다른 사용자의 리소스 접근 가능 여부

3. **입력 검증**
   - `@Valid`, `@Validated` 누락
   - DTO 필드에 `@NotNull`, `@Size`, `@Pattern` 등 제약조건 누락
   - 파일 업로드 시 확장자/크기/MIME 타입 검증

4. **시크릿 관리**
   - `application.yml`에 비밀번호, API 키 하드코딩
   - `.env` 파일이 `.gitignore`에 포함되어 있는지
   - 로그에 민감 정보 출력 여부

5. **CORS / CSRF**
   - CORS 허용 도메인이 `*`로 설정되어 있는지
   - CSRF 보호가 필요한데 비활성화되어 있는지
   - `SameSite` 쿠키 설정

6. **에러 응답**
   - 스택 트레이스가 클라이언트에 노출되는지
   - `@ControllerAdvice`에서 일관된 에러 응답 처리

### Frontend (Next.js)
1. **XSS**
   - `dangerouslySetInnerHTML` 사용 여부
   - 사용자 입력을 그대로 렌더링하는지
   - URL 파라미터를 sanitize 없이 사용하는지

2. **인증 토큰**
   - JWT를 `localStorage`에 저장하는지 (→ httpOnly 쿠키 권장)
   - 토큰 만료 처리 로직
   - 인증되지 않은 사용자의 페이지 접근 제어 (middleware)

3. **API 호출**
   - API 키가 클라이언트 번들에 포함되는지
   - Server Component / Route Handler를 통한 API 호출 여부
   - 환경변수에 `NEXT_PUBLIC_` 접두사 남용

4. **의존성**
   - 알려진 취약점이 있는 패키지 사용 여부
   - `npm audit` 결과 확인

## 출력 형식
```
## 🔴 Critical (즉시 수정)
- [파일:라인] 설명 → 수정 방법

## 🟡 Warning (권장 수정)
- [파일:라인] 설명 → 수정 방법

## 🟢 Info (개선 권장)
- [파일:라인] 설명 → 수정 방법
```

## 규칙
- 추측하지 말고 코드에서 확인된 것만 보고한다
- 각 항목에 구체적인 파일명과 라인을 명시한다
- 수정 방법은 코드 예시와 함께 제시한다
- 심각도 순서로 정렬한다
