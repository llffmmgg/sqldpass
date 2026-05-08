// Detect whether the page is running inside the Capacitor Android/iOS shell.
// Capacitor injects `window.Capacitor` automatically into its WebView; the web
// build never exposes it, so the absence of this global means "we're on the web".

export type CapacitorBrowserPlugin = {
  open: (options: { url: string; presentationStyle?: string }) => Promise<void>;
};

export type CapacitorPlugins = {
  Browser?: CapacitorBrowserPlugin;
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
