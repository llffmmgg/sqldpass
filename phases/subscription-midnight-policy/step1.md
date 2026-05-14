# Step 1 — 컨테이너 JVM TZ=Asia/Seoul

## 배경

운영 컨테이너 JVM TZ 가 UTC(기본). MySQL JDBC URL 만 `serverTimezone=Asia/Seoul`. `LocalDateTime.now()`/`LocalDate.now()`/`ZoneId.systemDefault()` 가 UTC 기준으로 동작 → 일자 그룹·통계·UI 표시 9시간 어긋남. JVM TZ 를 단일 진입점에서 KR 으로 통일.

## 작업 디렉터리

```
backend/
```

## 변경 대상

| 파일 | 변경 |
|------|------|
| `backend/Dockerfile` (runtime stage, L44 부근 `apt-get install` 블록) | `tzdata` 패키지 명시 + `ENV TZ=Asia/Seoul` 추가 |
| `.github/workflows/cd.yml` (docker run, L204 부근) | `-e TZ=Asia/Seoul \` 한 줄 추가 |
| `backend/src/main/resources/application.yaml` | `spring.jackson.time-zone: Asia/Seoul` 추가 (Instant/OffsetDateTime 직렬화 안전망) |

## Dockerfile 패치 (runtime stage)

```dockerfile
RUN apt-get update && apt-get install -y --no-install-recommends \
        tzdata \
        ...(기존 패키지)... \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Seoul
```

## cd.yml 패치

```yaml
docker run -d --name api ... \
  -e DB_URL=... \
  -e TZ=Asia/Seoul \
  ...
```

## application.yaml 패치

```yaml
spring:
  jackson:
    time-zone: Asia/Seoul
```

## 검증

```powershell
cd backend
.\gradlew.bat compileJava
.\gradlew.bat test
```

- `gradlew test` 통과 (회귀 확인 — TZ 변경이 단위 테스트에 영향 없는지)
- 배포 후 컨테이너 `date` 명령 KST 출력

## Acceptance Criteria

1. Dockerfile runtime stage 에 `tzdata` + `ENV TZ=Asia/Seoul` 추가.
2. cd.yml docker run 에 `-e TZ=Asia/Seoul` 추가.
3. application.yaml 에 `spring.jackson.time-zone: Asia/Seoul`.
4. `gradlew test` 전체 통과.

## 금지 사항

- JVM 시작 인자(`-Duser.timezone=Asia/Seoul`)로만 설정하지 마라. **이유**: ENTRYPOINT 가 시스템 TZ도 참조 — `date` 출력/로그 timestamp 불일치 위험. `TZ` 환경변수 + tzdata 가 표준.
- `ZoneId.of("Asia/Seoul")` 을 코드에 박지 마라. **이유**: 단일 진입점 일원화. 두 곳에 박히면 로컬/CI 분기 시 부정합.

## Status 규칙

- 성공: step 1 `completed`, summary "Dockerfile tzdata+ENV TZ=Asia/Seoul + cd.yml -e TZ + application.yaml jackson time-zone, gradle test OK".
- 실패: 3회 재시도 후 `error`.
