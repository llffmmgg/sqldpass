import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Docker 컨테이너 최적화 — /app/.next/standalone만 배포
  output: "standalone",
  // standalone 산출물 경로를 이 프로젝트(frontend/)로 고정.
  // 상위 디렉토리에 stale lockfile이 있어 monorepo root 오탐 → 경로 중첩되는 이슈 방지.
  // next build는 frontend/ 에서 실행되므로 process.cwd() === frontend path.
  outputFileTracingRoot: process.cwd(),
  poweredByHeader: false,
  images: {
    formats: ["image/avif", "image/webp"],
    minimumCacheTTL: 60 * 60 * 24 * 30, // 30일
  },
  experimental: {
    optimizePackageImports: ["react-markdown", "react-syntax-highlighter"],
  },
  compiler: {
    removeConsole:
      process.env.NODE_ENV === "production"
        ? { exclude: ["error", "warn"] }
        : false,
  },
  // OCI 운영: /api/* 는 nginx가 backend로 직접 라우팅 (rewrites 불필요)
  // 로컬 개발: NEXT_PUBLIC_API_URL로 기존대로 호출
  ...(process.env.NEXT_PUBLIC_API_URL
    ? {
        async rewrites() {
          return [
            {
              source: "/api/:path*",
              destination: `${process.env.NEXT_PUBLIC_API_URL}/api/:path*`,
            },
          ];
        },
      }
    : {}),
};

export default nextConfig;
