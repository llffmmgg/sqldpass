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
  "SQLD, 정보처리기사 실기 — IT 자격증을 실전 모의고사로 합격하세요. 매번 새로 생성되는 AI 문제, 오답 자동 복습, 회차별 실력 추적.";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "SQLD Pass — IT 자격증 실전 모의고사",
    template: "%s | SQLD Pass",
  },
  description: SITE_DESCRIPTION,
  keywords: [
    "SQLD",
    "SQLD 기출",
    "SQLD 문제",
    "SQLD 모의고사",
    "SQL 개발자",
    "정보처리기사 실기",
    "정처기 실기",
    "정처기 기출",
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
    title: "SQLD Pass — IT 자격증 실전 모의고사",
    description: SITE_DESCRIPTION,
    locale: "ko_KR",
  },
  twitter: {
    card: "summary_large_image",
    title: "SQLD Pass — IT 자격증 실전 모의고사",
    description: SITE_DESCRIPTION,
  },
  icons: {
    icon: "/favicon.svg",
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
