// 문어CBT 서비스 워커 — 안드로이드 앱(Capacitor 외부 URL 모드) 오프라인 지원의 보조 레이어.
// IndexedDB 가 콘텐츠(회차/문제)를 책임지고, 이 SW 는 정적 자산과 HTML 셸만 캐시한다.
//
// 정책:
//   /_next/static/*     CacheFirst (immutable 빌드 산출물)
//   이미지/폰트         CacheFirst
//   HTML navigation      NetworkFirst → 실패 시 캐시 → 그래도 없으면 /mock-exams 캐시
//   /api/*              건드리지 않음 (NetworkOnly) — IndexedDB 가 별도 처리
//   /sw.js, /manifest   건드리지 않음

const CACHE_VERSION = "v1";
const STATIC_CACHE = `sqldpass-static-${CACHE_VERSION}`;
const HTML_CACHE = `sqldpass-html-${CACHE_VERSION}`;

const STATIC_PATTERNS = [
  /^\/_next\/static\//,
  /^\/logo\//,
  /^\/og\//,
  /\.(?:png|webp|svg|jpg|jpeg|gif|ico|woff2?|ttf)$/,
];

self.addEventListener("install", (event) => {
  // 새 SW 가 즉시 활성화 — 사용자가 닫았다 다시 열 필요 없게.
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    (async () => {
      const keys = await caches.keys();
      await Promise.all(
        keys
          .filter((k) => !k.endsWith(`-${CACHE_VERSION}`))
          .map((k) => caches.delete(k)),
      );
      await self.clients.claim();
    })(),
  );
});

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  // API/SW/manifest 는 SW 가 손대지 않는다 — IndexedDB 와 매니페스트 fresh 보존.
  if (
    url.pathname.startsWith("/api/") ||
    url.pathname === "/sw.js" ||
    url.pathname === "/manifest.json"
  ) {
    return;
  }

  if (STATIC_PATTERNS.some((re) => re.test(url.pathname))) {
    event.respondWith(cacheFirst(request, STATIC_CACHE));
    return;
  }

  // HTML/페이지 네비게이션 — NetworkFirst.
  if (
    request.mode === "navigate" ||
    (request.headers.get("accept") || "").includes("text/html")
  ) {
    event.respondWith(networkFirst(request, HTML_CACHE));
    return;
  }
});

async function cacheFirst(request, cacheName) {
  const cache = await caches.open(cacheName);
  const hit = await cache.match(request);
  if (hit) return hit;
  try {
    const fresh = await fetch(request);
    if (fresh && fresh.ok) {
      cache.put(request, fresh.clone()).catch(() => {});
    }
    return fresh;
  } catch (e) {
    return new Response("", { status: 504, statusText: "offline" });
  }
}

async function networkFirst(request, cacheName) {
  const cache = await caches.open(cacheName);
  try {
    const fresh = await fetch(request);
    if (fresh && fresh.ok) {
      cache.put(request, fresh.clone()).catch(() => {});
    }
    return fresh;
  } catch (e) {
    const cached = await cache.match(request);
    if (cached) return cached;
    // 현재 URL 에 캐시가 없으면 모의고사 허브로라도 보내준다.
    const fallback = await cache.match("/mock-exams");
    if (fallback) return fallback;
    return new Response(
      "<!doctype html><meta charset=utf-8><title>오프라인</title>" +
        "<style>body{background:#0a0a0a;color:#eee;font-family:sans-serif;padding:24px}</style>" +
        "<h1>오프라인</h1><p>인터넷 연결을 확인한 뒤 다시 시도해주세요.</p>",
      { status: 503, headers: { "Content-Type": "text/html; charset=utf-8" } },
    );
  }
}
