"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import ExternalLink from "@/components/ExternalLink";

/**
 * 게시판 본문/댓글 markdown 렌더.
 * 보안: HTML raw 비허용 (rehype-raw 사용 X). 링크는 noopener noreferrer.
 */
export default function PostMarkdown({ content }: { content: string }) {
  return (
    <div className="post-markdown text-[0.95rem] leading-relaxed text-text">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          a: ({ href, children, ...props }) => (
            <ExternalLink
              href={href ?? "#"}
              className="text-primary underline underline-offset-2 hover:text-primary-hover"
              {...props}
            >
              {children}
            </ExternalLink>
          ),
          img: ({ src, alt }) => (
            // next/image 는 R2 도메인을 remotePatterns 에 등록해야 해서 plain img 사용.
            // 사용자 업로드 이미지는 어차피 외부에서 호스팅되므로 자동 최적화 의미 없음.
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={typeof src === "string" ? src : ""}
              alt={alt ?? ""}
              loading="lazy"
              className="my-4 max-w-full rounded-lg border border-border"
            />
          ),
          p: ({ children }) => <p className="my-3 whitespace-pre-wrap">{children}</p>,
          h1: ({ children }) => <h1 className="mt-6 mb-3 text-xl font-bold">{children}</h1>,
          h2: ({ children }) => <h2 className="mt-5 mb-2 text-lg font-bold">{children}</h2>,
          h3: ({ children }) => <h3 className="mt-4 mb-2 text-base font-semibold">{children}</h3>,
          ul: ({ children }) => <ul className="my-3 ml-5 list-disc space-y-1">{children}</ul>,
          ol: ({ children }) => <ol className="my-3 ml-5 list-decimal space-y-1">{children}</ol>,
          code: ({ children, ...props }) => (
            <code
              className="rounded border border-border bg-surface px-1.5 py-0.5 text-[0.85em]"
              {...props}
            >
              {children}
            </code>
          ),
          pre: ({ children }) => (
            <pre className="my-4 overflow-x-auto rounded-lg border border-border bg-surface p-3 text-sm">
              {children}
            </pre>
          ),
          blockquote: ({ children }) => (
            <blockquote className="my-3 border-l-4 border-primary/40 bg-surface px-4 py-2 text-text-muted">
              {children}
            </blockquote>
          ),
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}
