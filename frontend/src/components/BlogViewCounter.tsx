"use client";

import { useEffect } from "react";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "";

export default function BlogViewCounter({ slug }: { slug: string }) {
  useEffect(() => {
    fetch(`${API_URL}/api/public/blog/views/${slug}`, { method: "POST" }).catch(
      () => {},
    );
  }, [slug]);

  return null;
}
