"use client";

import { useEffect } from "react";

export default function BlogViewCounter({ slug }: { slug: string }) {
  useEffect(() => {
    fetch(`/api/public/blog/views/${slug}`, { method: "POST" }).catch(() => {});
  }, [slug]);

  return null;
}
