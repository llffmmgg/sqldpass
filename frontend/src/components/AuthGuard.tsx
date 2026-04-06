"use client";

import { useEffect, useState, type ReactNode } from "react";
import { isLoggedIn } from "@/lib/auth";
import LoginRequired from "@/components/LoginRequired";

export default function AuthGuard({ children }: { children: ReactNode }) {
  const [authChecked, setAuthChecked] = useState(false);
  const [loggedIn, setLoggedIn] = useState(false);

  useEffect(() => {
    setLoggedIn(isLoggedIn());
    setAuthChecked(true);
  }, []);

  if (!authChecked) return null;
  if (!loggedIn) return <LoginRequired />;
  return <>{children}</>;
}
