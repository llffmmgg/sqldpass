# ============================================================
# 1단계: 빌드 스테이지 (러너 네이티브 플랫폼에서 실행)
# - JDK 21 이미지에서 소스 코드를 컴파일하고 실행 가능한 JAR 생성
# - 이 스테이지는 최종 이미지에 포함되지 않음 (빌드 도구 제거)
# - --platform=$BUILDPLATFORM: GitHub 러너의 네이티브(x86)에서 빌드 → ARM 에뮬레이션 회피로 ~5배 빠름
# - Java 바이트코드는 플랫폼 무관하므로 x86에서 컴파일해도 ARM JRE에서 정상 동작
# ============================================================
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Gradle Wrapper + 빌드 설정 파일만 먼저 복사
# → 의존성 다운로드 레이어를 캐시하여 소스 변경 시 재다운로드 방지
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/

# Gradle Wrapper 실행 권한 부여 + 의존성 미리 다운로드 (캐시 레이어)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 코드 복사 (의존성 캐시 레이어 이후에 복사해야 캐시 효율 극대화)
COPY src/ src/

# 실행 가능한 JAR 빌드
# - 테스트는 CI 파이프라인에서 별도 실행하므로 여기서는 건너뜀 (-x test)
# - --no-daemon: 컨테이너 환경에서 Gradle 데몬 불필요
RUN ./gradlew bootJar --no-daemon -x test

# ============================================================
# 2단계: 런타임 스테이지
# - 빌드된 JAR만 가져와서 최소 크기의 실행 이미지 생성
# - JRE Alpine 기반 (~80MB) — JDK, Gradle 등 빌드 도구 미포함
# ============================================================
FROM eclipse-temurin:21-jre-alpine

# 보안: 비루트(non-root) 사용자 생성
# → 컨테이너가 탈취되더라도 호스트 시스템 권한 상승 방지
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# JAR 파일 소유권을 비루트 사용자로 변경
RUN chown appuser:appgroup app.jar

# 이후 모든 명령을 비루트 사용자로 실행
USER appuser

# Spring Boot 기본 포트
EXPOSE 8080

# JVM 실행 옵션:
# - UseContainerSupport: Docker 컨테이너의 메모리/CPU 제한을 JVM이 인식
# - MaxRAMPercentage=75.0: 컨테이너 메모리 제한의 75%를 JVM 힙에 할당
#   (나머지 25%는 메타스페이스, 스레드 스택, OS 등에 사용)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
