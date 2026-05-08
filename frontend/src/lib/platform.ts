// Detect whether the page is running inside the Capacitor Android/iOS shell.
// Capacitor injects `window.Capacitor` automatically into its WebView; the web
// build never exposes it, so the absence of this global means "we're on the web".

export type CapacitorBrowserPlugin = {
  open: (options: { url: string; presentationStyle?: string }) => Promise<void>;
};

/**
 * Play Billing 플러그인 인터페이스 (stub).
 * 실제 구현체는 mobile/ 워크스페이스의 Capacitor 플러그인 — 현재 미선정 상태.
 * 후보: Cap 6 다운 + codetrix-studio, Cap 8 업 + capacitor-firebase, 또는 커스텀 Java 래퍼.
 * 어느 쪽이든 아래 시그니처를 만족하도록 frontend 가 호출한다.
 */
export type CapacitorBillingPlugin = {
  /** 단발 인앱 결제 — Play Console 등록한 productId 로 결제창 호출. */
  purchase: (options: { productId: string }) => Promise<{
    success: boolean;
    purchaseToken?: string;
    orderId?: string;
    errorCode?: string;
    errorMessage?: string;
  }>;
};

export type CapacitorPlugins = {
  Browser?: CapacitorBrowserPlugin;
  Billing?: CapacitorBillingPlugin;
};

export type CapacitorGlobal = {
  isNativePlatform?: () => boolean;
  getPlatform?: () => "android" | "ios" | "web";
  platform?: string;
  Plugins?: CapacitorPlugins;
};

declare global {
  interface Window {
    Capacitor?: CapacitorGlobal;
  }
}

export function isCapacitorApp(): boolean {
  if (typeof window === "undefined") return false;
  const cap = window.Capacitor;
  if (!cap) return false;
  if (typeof cap.isNativePlatform === "function") return cap.isNativePlatform();
  return cap.platform === "android" || cap.platform === "ios";
}

export function getPlatform(): "android" | "ios" | "web" {
  if (typeof window === "undefined") return "web";
  const cap = window.Capacitor;
  if (cap?.getPlatform) return cap.getPlatform();
  if (cap?.platform === "android" || cap?.platform === "ios") return cap.platform;
  return "web";
}

export function isAndroidApp(): boolean {
  return isCapacitorApp() && getPlatform() === "android";
}

export function isIosApp(): boolean {
  return isCapacitorApp() && getPlatform() === "ios";
}
