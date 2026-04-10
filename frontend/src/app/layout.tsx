import type { Metadata } from "next";
import { Noto_Sans_KR, JetBrains_Mono, Caveat } from "next/font/google";
import Script from "next/script";
import { Suspense } from "react";
import NavBar from "@/components/NavBar";
import Footer from "@/components/Footer";
import { SiteNoticeBanner } from "@/components/SiteNoticeBanner";
import GAPageview from "@/components/GAPageview";
import "./globals.css";

const GA_ID = "G-MPQ2F9201M";
const ADSENSE_CLIENT = "ca-pub-6512792395955186";

const notoSansKr = Noto_Sans_KR({
  variable: "--font-sans-kr",
  subsets: ["latin"],
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
const SITE_NAME = "sqldpass";
const SITE_DESCRIPTION =
  "SQLD · 정보처리기사 실기 · 컴퓨터활용능력 1급 필기 무료 CBT 모의고사. 매번 새로 추가되는 기출 변형 문제, 오답 자동 복습, 회차별 실력 추적까지 한 곳에서.";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: "SQLD · 정처기 실기 · 컴활 1급 필기 무료 CBT 모의고사 | sqldpass",
    template: "%s | sqldpass",
  },
  description: SITE_DESCRIPTION,
  keywords: [
    // SQLD
    "SQLD",
    "SQLD CBT",
    "SQLD 모의고사",
    "SQLD 기출",
    "SQL 개발자",
    "SQL 개발자 자격증",
    // 정보처리기사 실기
    "정보처리기사 실기",
    "정보처리기사 CBT",
    "정보처리기사 실기 CBT",
    "정보처리기사 실기 모의고사",
    "정처기",
    "정처기 실기",
    "정처기 실기 CBT",
    "정처기 실기 모의고사",
    // 컴퓨터활용능력 1급
    "컴활 1급",
    "컴활 1급 필기",
    "컴활 필기 CBT",
    "컴활 모의고사",
    "컴퓨터활용능력 1급",
    "컴퓨터활용능력 1급 필기",
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
  },
  openGraph: {
    type: "website",
    url: SITE_URL,
    siteName: SITE_NAME,
    title: "SQLD · 정처기 실기 · 컴활 1급 필기 무료 CBT 모의고사",
    description: SITE_DESCRIPTION,
    locale: "ko_KR",
  },
  twitter: {
    card: "summary_large_image",
    title: "SQLD · 정처기 실기 · 컴활 1급 필기 무료 CBT 모의고사",
    description: SITE_DESCRIPTION,
  },
  verification: {
    google: "9wS1GcxcD3Gfl4FLj9PN9c5FdAnqKIudoectZCcf6KM",
    other: {
      "naver-site-verification": "baf127a946ae161a687576e193bd4a3e4e00e924",
    },
  },
  icons: {
    icon: "/logo/logo.png",
    apple: "/logo/logo.png",
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
      </head>
      <body className="min-h-full flex flex-col">
        <Script
          src={`https://www.googletagmanager.com/gtag/js?id=${GA_ID}`}
          strategy="afterInteractive"
        />
        <Script id="ga4-init" strategy="afterInteractive">
          {`
            window.dataLayer = window.dataLayer || [];
            function gtag(){dataLayer.push(arguments);}
            window.gtag = gtag;
            gtag('js', new Date());
            gtag('config', '${GA_ID}', { send_page_view: false });
          `}
        </Script>
        <Suspense fallback={null}>
          <GAPageview />
        </Suspense>
        <SiteNoticeBanner />
        <NavBar />
        <div className="flex-1">{children}</div>
        <Footer />
      </body>
    </html>
  );
}
