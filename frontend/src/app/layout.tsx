import type { Metadata } from "next";
import { Noto_Sans_KR, JetBrains_Mono, Caveat } from "next/font/google";
import { Suspense } from "react";
import NavBar from "@/components/NavBar";
import Footer from "@/components/Footer";
import { SiteNoticeBanner } from "@/components/SiteNoticeBanner";
import AnalyticsScripts from "@/components/AnalyticsScripts";
import AdSidebar from "@/components/AdSidebar";
import { ToastProvider } from "@/components/Toast";
import "./globals.css";

const ADSENSE_SIDEBAR_SLOT = process.env.NEXT_PUBLIC_ADSENSE_SIDEBAR_SLOT ?? "1606583562";

const ADSENSE_CLIENT = "ca-pub-6512792395955186";

const notoSansKr = Noto_Sans_KR({
  variable: "--font-sans-kr",
  subsets: ["latin", "latin-ext"],
  weight: ["400", "500", "600", "700"],
});

const jetbrainsMono = JetBrains_Mono({
  variable: "--font-jetbrains-mono",
  subsets: ["latin"],
});

const caveat = Caveat({
  variable: "--font-caveat",
  subsets: ["latin"],
  weight: ["700"],
});

const SITE_URL = "https://www.sqldpass.com";
const SITE_NAME = "문어CBT";
const SITE_TITLE = "SQLD · 정처기 · 컴활 · ADsP 무료 CBT 모의고사";
const SITE_DESCRIPTION =
  "CBT 모의고사 플랫폼 문어CBT. SQLD·정처기 필기/실기·컴활 1/2급·ADsP를 무료로 풀어보고, 기출 변형·오답 자동 복습·회차별 실력 추적까지 한 곳에서.";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: `${SITE_TITLE} | 문어CBT`,
    template: "%s | 문어CBT",
  },
  description: SITE_DESCRIPTION,
  keywords: [
    // SQLD
    "SQLD",
    "SQLD CBT",
    "SQLD 모의고사",
    "SQLD 기출",
    "SQL 개발자",
    // 정보처리기사 (필기/실기)
    "정보처리기사",
    "정보처리기사 필기",
    "정보처리기사 실기",
    "정처기",
    "정처기 필기",
    "정처기 실기",
    "정처기 CBT",
    "정처기 모의고사",
    // 컴퓨터활용능력 1·2급
    "컴활",
    "컴활 1급",
    "컴활 2급",
    "컴활 필기",
    "컴활 실기",
    "컴퓨터활용능력",
    "컴퓨터활용능력 1급",
    "컴퓨터활용능력 2급",
    // ADsP
    "ADsP",
    "ADsP CBT",
    "ADsP 모의고사",
    "데이터분석 준전문가",
    // 공통
    "자격증 CBT",
    "무료 CBT",
    "자격증 모의고사",
    "자격증 기출",
    "IT 자격증",
    "랜덤 모의고사",
  ],
  authors: [{ name: "sqldpass" }],
  alternates: {
    canonical: SITE_URL,
    languages: { ko: SITE_URL, "x-default": SITE_URL },
  },
  openGraph: {
    type: "website",
    url: SITE_URL,
    siteName: SITE_NAME,
    title: SITE_TITLE,
    description: SITE_DESCRIPTION,
    locale: "ko_KR",
  },
  twitter: {
    card: "summary_large_image",
    title: SITE_TITLE,
    description: SITE_DESCRIPTION,
  },
  verification: {
    google: "9wS1GcxcD3Gfl4FLj9PN9c5FdAnqKIudoectZCcf6KM",
    other: {
      "naver-site-verification": "baf127a946ae161a687576e193bd4a3e4e00e924",
    },
  },
  icons: {
    icon: "/logo/favicon.png",
    apple: "/logo/favicon.png",
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
    logo: `${SITE_URL}/logo/logo.png`,
    sameAs: [] as string[],
  };

  const websiteLd = {
    "@context": "https://schema.org",
    "@type": "WebSite",
    name: "문어CBT 다양한 자격증 기출문제 전자문제집 CBT",
    alternateName: SITE_NAME,
    url: SITE_URL,
    description: SITE_DESCRIPTION,
    inLanguage: "ko-KR",
    potentialAction: {
      "@type": "SearchAction",
      target: `${SITE_URL}/learn?q={search_term_string}`,
      "query-input": "required name=search_term_string",
    },
  };

  const siteNavLd = {
    "@context": "https://schema.org",
    "@type": "ItemList",
    itemListElement: [
      { "@type": "SiteNavigationElement", position: 1, name: "문제 풀기", url: `${SITE_URL}/solve` },
      { "@type": "SiteNavigationElement", position: 2, name: "모의고사", url: `${SITE_URL}/mock-exams` },
      { "@type": "SiteNavigationElement", position: 3, name: "기출문제", url: `${SITE_URL}/learn` },
      { "@type": "SiteNavigationElement", position: 4, name: "대시보드", url: `${SITE_URL}/dashboard` },
      { "@type": "SiteNavigationElement", position: 5, name: "오답 노트", url: `${SITE_URL}/wrong-answers` },
      { "@type": "SiteNavigationElement", position: 6, name: "소개", url: `${SITE_URL}/about` },
    ],
  };

  return (
    <html
      lang="ko"
      className={`${notoSansKr.variable} ${jetbrainsMono.variable} ${caveat.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        <meta name="google-adsense-account" content={ADSENSE_CLIENT} />
        <script
          async
          src={`https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${ADSENSE_CLIENT}`}
          crossOrigin="anonymous"
        />
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(organizationLd) }}
        />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(websiteLd) }}
        />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(siteNavLd) }}
        />
        <link rel="manifest" href="/manifest.json" />
      </head>
      <body className="min-h-full flex flex-col">
        <Suspense fallback={null}>
          <AnalyticsScripts />
        </Suspense>
        <ToastProvider>
          <SiteNoticeBanner />
          <NavBar />
          <div className="flex-1">{children}</div>
          <Footer />
          <AdSidebar adSlot={ADSENSE_SIDEBAR_SLOT} />
        </ToastProvider>
      </body>
    </html>
  );
}
