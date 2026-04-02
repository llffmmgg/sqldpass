"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { isAuthenticated, clearToken } from "@/lib/adminApi";

const SIDEBAR_LINKS = [
  { href: "/admin", label: "대시보드", icon: "📊" },
  { href: "/admin/questions", label: "문제 관리", icon: "📝" },
  { href: "/admin/members", label: "회원 관리", icon: "👥" },
  { href: "/admin/generate", label: "문제 생성", icon: "🤖" },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [checked, setChecked] = useState(false);

  useEffect(() => {
    if (pathname === "/admin/login") {
      setChecked(true);
      return;
    }
    if (!isAuthenticated()) {
      router.replace("/admin/login");
      return;
    }
    setChecked(true);
  }, [pathname, router]);

  if (!checked) return null;

  if (pathname === "/admin/login") {
    return <>{children}</>;
  }

  function handleLogout() {
    clearToken();
    router.push("/admin/login");
  }

  return (
    <div className="flex min-h-screen bg-background text-foreground">
      {/* Sidebar */}
      <aside className="w-56 shrink-0 border-r border-border bg-surface p-4">
        <Link href="/admin" className="block text-lg font-bold">
          SQLD <span className="text-primary">Admin</span>
        </Link>

        <nav className="mt-6 space-y-1">
          {SIDEBAR_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                (link.href === "/admin" ? pathname === "/admin" : pathname.startsWith(link.href))
                  ? "bg-primary/10 text-primary"
                  : "text-muted hover:text-foreground"
              }`}
            >
              <span>{link.icon}</span>
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="mt-auto pt-6">
          <button
            onClick={handleLogout}
            className="w-full rounded-lg border border-border px-3 py-2 text-sm text-muted transition hover:text-foreground"
          >
            로그아웃
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 p-8">{children}</main>
    </div>
  );
}
