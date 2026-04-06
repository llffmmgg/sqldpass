import type { Metadata } from "next";
import { IBM_Plex_Sans_KR, Fraunces, JetBrains_Mono } from "next/font/google";
import NavBar from "@/components/NavBar";
import "./globals.css";

const ibmPlexSansKr = IBM_Plex_Sans_KR({
  variable: "--font-sans-kr",
  subsets: ["latin"],
  weight: ["300", "400", "500", "600", "700"],
});

const fraunces = Fraunces({
  variable: "--font-fraunces",
  subsets: ["latin"],
  axes: ["SOFT", "opsz"],
});

const jetbrainsMono = JetBrains_Mono({
  variable: "--font-jetbrains-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "SQLD Pass - SQLD 자격증 시험 대비 문제 풀이",
  description:
    "실제 시험과 유사한 문제로 SQLD 자격증을 준비하세요. 풀이 기록, 오답 분석까지.",
  icons: {
    icon: "/favicon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const themeScript = `(function(){try{var t=localStorage.getItem('theme');if(t==='dark'||(!t&&window.matchMedia('(prefers-color-scheme:dark)').matches)){document.documentElement.classList.add('dark')}else{document.documentElement.classList.remove('dark')}}catch(e){document.documentElement.classList.add('dark')}})()`;

  return (
    <html
      lang="ko"
      className={`${ibmPlexSansKr.variable} ${fraunces.variable} ${jetbrainsMono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </head>
      <body className="min-h-full flex flex-col">
        <NavBar />
        <div className="flex-1">{children}</div>
      </body>
    </html>
  );
}
