# ============================================================
# 1단계: 의존성 설치 (BUILDPLATFORM에서 실행)
# - GitHub 러너의 네이티브(x86)에서 npm ci → ARM 에뮬레이션 회피
# - node_modules만 레이어 캐싱
# ============================================================
FROM --platform=$BUILDPLATFORM node:20-alpine AS deps

WORKDIR /app

# package 파일만 먼저 복사 (소스 변경 시 재설치 방지)
COPY package.json package-lock.json* ./

# libc6-compat: Alpine musl 환경에서 sharp(next/image) 네이티브 바인딩 호환
RUN apk add --no-cache libc6-compat \
 && npm ci --no-audit --no-fund

# ============================================================
# 2단계: 빌드 (BUILDPLATFORM에서 실행)
# - Next.js standalone 산출물 생성
# - 서버 코드(JS)는 플랫폼 무관 → x86에서 빌드해도 ARM에서 실행 가능
# ============================================================
FROM --platform=$BUILDPLATFORM node:20-alpine AS builder

WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# ============================================================
# 3단계: 런타임 (TARGETPLATFORM = linux/arm64)
# - standalone 산출물만 복사하여 이미지 최소화
# - non-root user로 실행
# ============================================================
FROM node:20-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0

# sharp 런타임 호환 패키지
RUN apk add --no-cache libc6-compat \
 && addgroup -S appgroup \
 && adduser -S appuser -G appgroup

# standalone 산출물 — server.js + 최소 node_modules 포함
COPY --from=builder --chown=appuser:appgroup /app/public ./public
COPY --from=builder --chown=appuser:appgroup /app/.next/standalone ./
COPY --from=builder --chown=appuser:appgroup /app/.next/static ./.next/static

USER appuser

EXPOSE 3000

CMD ["node", "server.js"]
