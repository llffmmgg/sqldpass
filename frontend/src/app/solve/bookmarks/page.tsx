import type { Metadata } from "next";
import { Suspense } from "react";

import Spinner from "@/components/Spinner";
import { Container } from "@/components/ui";
import BookmarksSolveClient from "./BookmarksSolveClient";

export const metadata: Metadata = {
  title: "즐겨찾기 모아 풀기",
  description: "즐겨찾기 한 문제들을 랜덤으로 섞어 10문제씩 풀고 오답은 자동으로 오답노트에 누적돼요.",
};

export default function Page() {
  return (
    <Container className="py-10">
      <Suspense
        fallback={
          <div className="flex min-h-[40vh] items-center justify-center">
            <Spinner message="즐겨찾기 불러오는 중..." />
          </div>
        }
      >
        <BookmarksSolveClient />
      </Suspense>
    </Container>
  );
}
