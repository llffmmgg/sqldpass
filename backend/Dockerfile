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
# 2단계: 런타임 스테이지 (Debian/Ubuntu 기반 — Playwright 호환)
# - 모의고사 PDF 생성에 Playwright + 헤드리스 Chromium 사용 → musl(alpine) 미지원
# - eclipse-temurin:21-jre-jammy = Ubuntu 22.04 + JRE 21
# ============================================================
FROM eclipse-temurin:21-jre-jammy

# 한글 폰트 + Chromium 시스템 의존성 + Playwright CLI 가 사용할 ca-certs/curl
# - fonts-noto-cjk: PDF 본문 한글 렌더 (없으면 자모 분리)
# - libnss3 외 다수: Chromium headless 실행 필수 라이브러리
RUN apt-get update && apt-get install -y --no-install-recommends \
        fonts-noto-cjk fonts-noto-color-emoji \
        ca-certificates curl \
        libnss3 libatk1.0-0 libatk-bridge2.0-0 libcups2 \
        libxkbcommon0 libxcomposite1 libxdamage1 libxrandr2 \
        libgbm1 libpango-1.0-0 libcairo2 libasound2 \
        libxshmfence1 libdrm2 \
    && rm -rf /var/lib/apt/lists/*

# Playwright Chromium 캐시 위치 — appuser 가 쓸 수 있는 공유 경로.
# (HOME 이 /root 가 아닌 사용자라도 동일 경로에서 찾도록 명시.)
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
ENV PDF_VERIFY_ON_STARTUP=true

# 비루트 사용자
RUN groupadd -r appgroup && useradd -r -g appgroup -m -d /home/appuser appuser \
 && mkdir -p /ms-playwright \
 && chown -R appuser:appgroup /ms-playwright

WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일 복사
COPY --from=build --chown=appuser:appgroup /app/build/libs/*.jar app.jar

# Playwright Chromium 사전 설치 (이미지 빌드 시 1회) — 첫 PDF 요청 지연 최소화.
# Spring Boot fat-jar 에서 Playwright CLI 클래스를 호출하기 위해 jarmode tools 로 의존성을 추출,
# 추출된 BOOT-INF/lib 의 jar 들을 클래스패스로 잡아 com.microsoft.playwright.CLI install 실행.
USER appuser
RUN java -Djarmode=tools -jar app.jar extract --destination /tmp/extracted \
 && java -cp '/tmp/extracted/lib/*' com.microsoft.playwright.CLI install chromium \
 && rm -rf /tmp/extracted

# Spring Boot 기본 포트
EXPOSE 8080

# JVM 실행 옵션:
# - UseContainerSupport: Docker 컨테이너의 메모리/CPU 제한을 JVM이 인식
# - MaxRAMPercentage=75.0: 컨테이너 메모리 제한의 75%를 JVM 힙에 할당
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
