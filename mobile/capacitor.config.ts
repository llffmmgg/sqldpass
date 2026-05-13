import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.sqldpass.app",
  appName: "문어CBT",
  webDir: "web",
  server: {
    url: "https://www.sqldpass.com",
    androidScheme: "https",
    cleartext: false,
    allowNavigation: ["www.sqldpass.com", "sqldpass.com"],
  },
  android: {
    allowMixedContent: false,
    backgroundColor: "#0a0a0a",
    // debug 빌드(NODE_ENV !== "production")에서만 Chrome chrome://inspect 접근 허용.
    // release 빌드는 반드시 false 로 유지 — WebView remote debugging 노출 시
    // 인앱 코드/세션 토큰 누출 위험. release 빌드 시 NODE_ENV=production 으로 cap sync.
    webContentsDebuggingEnabled: process.env.NODE_ENV !== "production",
  },
  plugins: {
    PushNotifications: {
      presentationOptions: ["badge", "sound", "alert"],
    },
  },
};

export default config;
