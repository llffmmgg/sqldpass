// AdSense ads.txt — 도메인 검증용 정적 응답.
export const dynamic = "force-static";

export async function GET() {
  return new Response(
    "google.com, pub-6512792395955186, DIRECT, f08c47fec0942fa0\n",
    {
      headers: { "Content-Type": "text/plain; charset=utf-8" },
    },
  );
}
