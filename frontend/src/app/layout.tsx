import type { Metadata } from "next";
import { Noto_Sans_KR, JetBrains_Mono } from "next/font/google";
import NavBar from "@/components/NavBar";
import "./globals.css";

const notoSansKr = Noto_Sans_KR({
  variable: "--font-sans-kr",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

const jetbrainsMono = JetBrains_Mono({
  variable: "--font-jetbrains-mono",
  subsets: ["latin"],
});

const SITE_URL = "https://www.sqldpass.com";
const SITE_NAME = "SQLD Pass";
const SITE_DESCRIPTION =
  "SQLD CBT · 정보처리기사 실기 CBT 무료 모의고사. 실제 시험과 동일한 환경에서 매번 새로 생성되는 AI 기출 문제를 풀고, 오답 자동 복습과 회차별 실력 추적까지. 무료로 시작하세요.";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "SQLD CBT 모의고사 · 정보처리기사 실기 CBT 무료 문제풀이 | SQLD Pass",
    template: "%s | SQLD Pass",
  },
  description: SITE_DESCRIPTION,
  keywords: [
    "SQLD",
    "SQLD CBT",
    "SQLD 모의고사",
    "SQLD 기출",
    "SQLD 기출문제",
    "SQLD 문제",
    "SQLD 무료",
    "SQL 개발자",
    "SQL 개발자 자격증",
    "정보처리기사 CBT",
    "정보처리기사 실기",
    "정보처리기사 실기 CBT",
    "정보처리기사 실기 모의고사",
    "정보처리기사 실기 기출",
    "정처기",
    "정처기 실기",
    "정처기 실기 CBT",
    "정처기 실기 모의고사",
    "정처기 실기 기출",
    "정처기 CBT",
    "CBT",
    "무료 CBT",
    "IT 자격증",
    "자격증 문제집",
    "AI 모의고사",
  ],
  authors: [{ name: "SQLD Pass" }],
  alternates: {
    canonical: SITE_URL,
  },
  openGraph: {
    type: "website",
    url: SITE_URL,
    siteName: SITE_NAME,
    title: "SQLD CBT · 정처기 실기 CBT 무료 모의고사 | SQLD Pass",
    description: SITE_DESCRIPTION,
    locale: "ko_KR",
  },
  twitter: {
    card: "summary_large_image",
    title: "SQLD CBT · 정처기 실기 CBT 무료 모의고사 | SQLD Pass",
    description: SITE_DESCRIPTION,
  },
  verification: {
    google: "9wS1GcxcD3Gfl4FLj9PN9c5FdAnqKIudoectZCcf6KM",
    other: {
      "naver-site-verification": "baf127a946ae161a687576e193bd4a3e4e00e924",
    },
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-image-preview": "large",
      "max-snippet": -1,
    },
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const themeScript = `(function(){try{var t=localStorage.getItem('theme');if(t==='dark'||(!t&&window.matchMedia('(prefers-color-scheme:dark)').matches)){document.documentElement.classList.add('dark')}else{document.documentElement.classList.remove('dark')}}catch(e){document.documentElement.classList.add('dark')}})()`;

  const organizationLd = {
    "@context": "https://schema.org",
    "@type": "Organization",
    name: SITE_NAME,
    url: SITE_URL,
    logo: `${SITE_URL}/favicon.svg`,
    sameAs: [] as string[],
  };

  const websiteLd = {
    "@context": "https://schema.org",
    "@type": "WebSite",
    name: SITE_NAME,
    url: SITE_URL,
    description: SITE_DESCRIPTION,
    inLanguage: "ko-KR",
    potentialAction: {
      "@type": "SearchAction",
      target: `${SITE_URL}/learn?q={search_term_string}`,
      "query-input": "required name=search_term_string",
    },
  };

  return (
    <html
      lang="ko"
      className={`${notoSansKr.variable} ${jetbrainsMono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(organizationLd) }}
        />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(websiteLd) }}
        />
      </head>
      <body className="min-h-full flex flex-col">
        <NavBar />
        <div className="flex-1">{children}</div>
      </body>
    </html>
  );
}
