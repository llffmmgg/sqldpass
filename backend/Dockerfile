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

# Playwright CLI 호출에 필요한 의존성 jar 디렉토리만 따로 추출.
# - runtime stage 의 install layer 가 build.gradle 변경 시에만 무효화되어
#   소스 변경 배포에서는 Chromium 재다운로드 없이 layer cache hit.
# - extract 결과: /app/extracted/lib/* 에 fat-jar 의 모든 dependency jar.
RUN java -Djarmode=tools -jar build/libs/*.jar extract --destination /app/extracted

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
        libxkbcommon0 libxcomposite1 libxdamage1 libxrandr2 libxfixes3 \
        libgbm1 libpango-1.0-0 libcairo2 libasound2 \
        libxshmfence1 libdrm2 \
    && rm -rf /var/lib/apt/lists/*

# Playwright Chromium 캐시 위치 — appuser 가 쓸 수 있는 공유 경로.
# (HOME 이 /root 가 아닌 사용자라도 동일 경로에서 찾도록 명시.)
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
# 런타임에 NodeJS 드라이버가 chromium 외 firefox/webkit 까지 자동 다운로드하는 동작 차단.
# (자동 다운로드는 헬스체크 90s 안에 못 끝나서 신규 컨테이너 기동 실패의 원인이었음)
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
ENV PDF_VERIFY_ON_STARTUP=true

# 비루트 사용자
RUN groupadd -r appgroup && useradd -r -g appgroup -m -d /home/appuser appuser \
 && mkdir -p /ms-playwright \
 && chown -R appuser:appgroup /ms-playwright

WORKDIR /app

# 의존성 lib 디렉토리만 먼저 COPY — Chromium install 의 layer cache 키가 됨.
# build.gradle 변경 시에만 hash 가 변경되어 install 이 재실행되고,
# 일반 소스 변경에는 캐시 hit 으로 Chromium 재다운로드를 건너뜀.
COPY --from=build --chown=appuser:appgroup /app/extracted/lib/ /opt/pw-lib/

# Playwright Chromium 사전 설치 — 위 lib 디렉토리가 동일하면 layer cache hit.
USER appuser
RUN java -cp '/opt/pw-lib/*' com.microsoft.playwright.CLI install chromium

# 빌드 산출물(매 빌드 변경) 은 install 이후에 COPY — install 캐시 보호.
USER root
COPY --from=build --chown=appuser:appgroup /app/build/libs/*.jar app.jar
USER appuser

# Spring Boot 기본 포트
EXPOSE 8080

# JVM 실행 옵션:
# - UseContainerSupport: Docker 컨테이너의 메모리/CPU 제한을 JVM이 인식
# - MaxRAMPercentage=75.0: 컨테이너 메모리 제한의 75%를 JVM 힙에 할당
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
