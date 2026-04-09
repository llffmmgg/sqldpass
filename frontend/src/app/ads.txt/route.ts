// AdSense ads.txt — 도메인 검증용 정적 응답.
// 승인 후 publisher ID로 placeholder 라인을 교체:
//   google.com, pub-XXXXXXXXXXXXXXXX, DIRECT, f08c47fec0942fa0
export const dynamic = "force-static";

export async function GET() {
  return new Response("# placeholder — AdSense 승인 후 publisher ID로 교체\n", {
    headers: { "Content-Type": "text/plain; charset=utf-8" },
  });
}
