"use client";

import { useEffect } from "react";

const KEY = "sqldpass:lastCert";

export default function RememberCert({ slug }: { slug: string }) {
  useEffect(() => {
    try {
      window.localStorage.setItem(KEY, slug);
    } catch {
      /* private mode 등에서 storage 차단 — 무시 */
    }
  }, [slug]);
  return null;
}
