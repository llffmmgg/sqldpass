---
name: performance-review
description: Spring Boot 백엔드와 Next.js 프론트엔드의 성능 병목을 분석한다. "성능 리뷰", "성능 점검", "성능 확인", "느린 쿼리", "N+1 확인", "번들 사이즈", "렌더링 최적화", "병목 찾아줘" 등의 요청이 있을 때 사용.
---

# 성능 리뷰 스킬 (Spring Boot + Next.js)

## 역할
Spring Boot 백엔드와 Next.js 프론트엔드 코드베이스의 성능 병목을 분석한다.

## 리뷰 항목

### Backend (Spring Boot)
1. **N+1 쿼리**
   - `@OneToMany`, `@ManyToOne` 관계에서 `FetchType.EAGER` 사용
   - 컬렉션 조회 시 `@EntityGraph` 또는 `JOIN FETCH` 누락
   - 반복문 안에서 추가 쿼리 발생 패턴

2. **DB 인덱스**
   - `WHERE`, `ORDER BY`, `JOIN`에 사용되는 컬럼의 `@Index` 누락
   - 복합 인덱스가 필요한 쿼리 패턴
   - `LIKE '%keyword%'` 같은 풀스캔 유발 쿼리

3. **쿼리 최적화**
   - `SELECT *` 대신 필요한 컬럼만 조회 (Projection, DTO 직접 조회)
   - 페이징 없는 대량 데이터 조회
   - `COUNT` 쿼리 최적화 (`exists` 사용 가능 여부)

4. **캐싱**
   - 자주 조회되고 변경이 적은 데이터에 `@Cacheable` 누락
   - 캐시 무효화 전략 (`@CacheEvict`) 확인
   - 불필요한 캐시 사용 (자주 변경되는 데이터)

5. **커넥션 풀**
   - HikariCP 설정 확인 (`maximum-pool-size`, `connection-timeout`)
   - 커넥션 누수 패턴 (트랜잭션 미종료, try-with-resources 누락)

6. **비동기 처리**
   - 오래 걸리는 작업(이메일, 파일 처리)이 동기로 처리되는지
   - `@Async` 또는 메시지 큐 사용 권장 여부

7. **메모리**
   - 대용량 컬렉션을 메모리에 전부 로드하는 패턴
   - Stream API로 변환 가능한 반복 처리
   - 불필요한 객체 생성 (반복문 안에서 `new`)

### Frontend (Next.js)
1. **렌더링 전략**
   - SSR이 필요없는 페이지가 SSR로 되어 있는지
   - 정적 생성 가능한 페이지에 `generateStaticParams` 누락
   - `use client` 남용 — Server Component로 가능한 부분

2. **재렌더링**
   - 부모 컴포넌트 상태 변경으로 자식 전체 재렌더링
   - `useMemo`, `useCallback` 필요한 곳에 누락
   - `key` prop 누락 또는 index를 key로 사용

3. **번들 사이즈**
   - 무거운 라이브러리 전체 import (`import moment` → `dayjs` 또는 tree-shaking)
   - dynamic import (`next/dynamic`) 활용 가능 여부
   - 이미지 최적화 (`next/image` 미사용)

4. **데이터 페칭**
   - 워터폴 요청 (순차 fetch → 병렬 `Promise.all`)
   - 동일 데이터 중복 요청
   - SWR/React Query 캐싱 전략

5. **이미지/폰트**
   - `next/image` 미사용 (width, height, lazy loading)
   - 폰트 최적화 (`next/font` 미사용)
   - 불필요한 대용량 이미지

## 출력 형식
```
## 🔴 Critical (성능 심각 영향)
- [파일:라인] 설명
  - 현재: (문제 코드)
  - 개선: (수정 코드)
  - 예상 효과: (개선 수치 또는 설명)

## 🟡 Warning (개선 권장)
- [파일:라인] 설명 → 수정 방법

## 🟢 Info (최적화 가능)
- [파일:라인] 설명 → 수정 방법
```

## 규칙
- 추측하지 말고 코드에서 확인된 것만 보고한다
- 각 항목에 구체적인 파일명과 라인을 명시한다
- 수정 전/후 코드를 비교하여 제시한다
- 성능 영향도가 큰 순서로 정렬한다
- 마이크로 최적화보다 실질적 병목에 집중한다
