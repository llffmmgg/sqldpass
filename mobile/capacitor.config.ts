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
    webContentsDebuggingEnabled: false,
  },
  plugins: {
    PushNotifications: {
      presentationOptions: ["badge", "sound", "alert"],
    },
  },
};

export default config;
