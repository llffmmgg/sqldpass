import type { MetadataRoute } from "next";

const SITE_URL = "https://www.sqldpass.com";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: "*",
        allow: "/",
        disallow: [
          "/admin",
          "/admin/",
          "/auth/",
          "/api/",
          "/dashboard",
          "/dashboard/",
          "/wrong-answers",
          "/wrong-answers/",
          "/profile",
          "/history",
          "/history/",
          "/mypage/",
        ],
      },
    ],
    sitemap: `${SITE_URL}/sitemap.xml`,
    host: SITE_URL,
  };
}
