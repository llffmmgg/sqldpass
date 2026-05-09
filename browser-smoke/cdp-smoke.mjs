import { spawn } from "node:child_process";
import { mkdir, rm } from "node:fs/promises";
import path from "node:path";

const chromePath = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const outDir = path.resolve("browser-smoke");
const profileDir = path.join(outDir, "cdp-profile");
const port = 9333;
const baseUrl = "https://www.sqldpass.com";

await rm(profileDir, { recursive: true, force: true });
await mkdir(profileDir, { recursive: true });

const chrome = spawn(chromePath, [
  "--headless=new",
  "--disable-gpu",
  "--no-first-run",
  "--no-default-browser-check",
  `--user-data-dir=${profileDir}`,
  `--remote-debugging-port=${port}`,
  "about:blank",
], { stdio: "ignore", windowsHide: true });

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function waitForJson(url, timeoutMs = 10000) {
  const started = Date.now();
  let lastError;
  while (Date.now() - started < timeoutMs) {
    try {
      const response = await fetch(url);
      if (response.ok) return response.json();
      lastError = new Error(`HTTP ${response.status}`);
    } catch (error) {
      lastError = error;
    }
    await wait(100);
  }
  throw lastError ?? new Error(`Timed out waiting for ${url}`);
}

await waitForJson(`http://127.0.0.1:${port}/json/version`);
const targetResponse = await fetch(`http://127.0.0.1:${port}/json/new?about:blank`, {
  method: "PUT",
});
if (!targetResponse.ok) {
  throw new Error(`Failed to create Chrome target: HTTP ${targetResponse.status}`);
}
const target = await targetResponse.json();
const ws = new WebSocket(target.webSocketDebuggerUrl);
let nextId = 1;
const pending = new Map();
const events = [];

ws.addEventListener("message", (event) => {
  const message = JSON.parse(event.data);
  if (message.id && pending.has(message.id)) {
    const { resolve, reject } = pending.get(message.id);
    pending.delete(message.id);
    if (message.error) reject(new Error(message.error.message));
    else resolve(message.result);
    return;
  }
  if (message.method) events.push(message);
});

await new Promise((resolve, reject) => {
  ws.addEventListener("open", resolve, { once: true });
  ws.addEventListener("error", reject, { once: true });
});

function send(method, params = {}) {
  const id = nextId++;
  ws.send(JSON.stringify({ id, method, params }));
  return new Promise((resolve, reject) => pending.set(id, { resolve, reject }));
}

async function evaluate(expression) {
  const result = await send("Runtime.evaluate", {
    expression,
    awaitPromise: true,
    returnByValue: true,
  });
  if (result.exceptionDetails) {
    return { error: result.exceptionDetails.text };
  }
  return result.result.value;
}

async function navigate(pathname, viewport) {
  events.length = 0;
  if (viewport) {
    await send("Emulation.setDeviceMetricsOverride", {
      width: viewport.width,
      height: viewport.height,
      deviceScaleFactor: 1,
      mobile: viewport.mobile,
    });
  }
  await send("Page.navigate", { url: `${baseUrl}${pathname}` });
  await new Promise((resolve) => {
    const started = Date.now();
    const timer = setInterval(() => {
      const loaded = events.some((event) => event.method === "Page.loadEventFired");
      if (loaded || Date.now() - started > 15000) {
        clearInterval(timer);
        resolve();
      }
    }, 100);
  });
  await wait(2500);
}

async function snapshot(name) {
  return {
    name,
    url: await evaluate("location.href"),
    title: await evaluate("document.title"),
    h1: await evaluate("document.querySelector('h1')?.innerText ?? ''"),
    bodyHasLogin: await evaluate("document.body.innerText.includes('로그인')"),
    jsErrors: events
      .filter((event) => event.method === "Runtime.exceptionThrown")
      .map((event) => event.params.exceptionDetails.text),
    httpErrors: events
      .filter((event) => event.method === "Network.responseReceived" && event.params.response.status >= 400)
      .map((event) => `${event.params.response.status} ${event.params.response.url}`),
    failedRequests: events
      .filter((event) => event.method === "Network.loadingFailed")
      .map((event) => `${event.params.errorText} ${event.params.requestId} ${event.params.blockedReason ?? ""}`.trim()),
  };
}

function collectFailures(results) {
  const failures = [];

  for (const result of results) {
    if (Array.isArray(result.jsErrors) && result.jsErrors.length > 0) {
      failures.push(`${result.name}: runtime exceptions: ${result.jsErrors.join("; ")}`);
    }
    if (Array.isArray(result.httpErrors) && result.httpErrors.length > 0) {
      failures.push(`${result.name}: HTTP errors: ${result.httpErrors.join("; ")}`);
    }
    if (Array.isArray(result.failedRequests) && result.failedRequests.length > 0) {
      failures.push(`${result.name}: failed requests: ${result.failedRequests.join("; ")}`);
    }
    if (result.name === "home cta click" && result.clicked !== true) {
      failures.push(`${result.name}: CTA was not found or not clicked`);
    }
    if (result.name === "solve modal" && result.hasModal !== true) {
      failures.push(`${result.name}: expected modal was not detected`);
    }
    if (result.hasHorizontalOverflow === true) {
      failures.push(`${result.name}: mobile horizontal overflow detected`);
    }
  }

  return failures;
}

try {
  await send("Page.enable");
  await send("Runtime.enable");
  await send("Network.enable");

  const results = [];

  await navigate("/", { width: 1440, height: 1200, mobile: false });
  results.push(await snapshot("desktop home"));
  results.push({
    name: "home cta click",
    beforeUrl: await evaluate("location.href"),
    clicked: await evaluate(`(() => {
      const links = [...document.querySelectorAll('a,button')];
      const target = links.find((el) => el.innerText.includes('무료로 시작하기'));
      if (!target) return false;
      target.click();
      return true;
    })()`),
  });
  await wait(2000);
  results.at(-1).afterUrl = await evaluate("location.href");

  await navigate("/solve?cert=SQLD", { width: 1365, height: 1000, mobile: false });
  results.push(await snapshot("solve sqld"));
  results.push({
    name: "solve modal",
    hasModal: await evaluate("document.body.innerText.includes('시작 전에 잠깐만요')"),
  });

  await navigate("/mock-exams?cert=SQLD", { width: 1365, height: 1000, mobile: false });
  results.push(await snapshot("mock exams"));

  await navigate("/past-exams/sqld", { width: 1365, height: 1000, mobile: false });
  results.push(await snapshot("past exams sqld"));

  await navigate("/dashboard", { width: 1365, height: 1000, mobile: false });
  results.push(await snapshot("dashboard logged out"));

  await navigate("/", { width: 390, height: 844, mobile: true });
  results.push({
    ...(await snapshot("mobile home")),
    viewportWidth: await evaluate("document.documentElement.clientWidth"),
    scrollWidth: await evaluate("document.documentElement.scrollWidth"),
    hasHorizontalOverflow: await evaluate("document.documentElement.scrollWidth > document.documentElement.clientWidth"),
    overflowingText: await evaluate(`(() => {
      const vw = document.documentElement.clientWidth;
      return [...document.querySelectorAll('h1,p,a,button,section,div')]
        .filter((el) => {
          const rect = el.getBoundingClientRect();
          return rect.width > 0 && (rect.left < -1 || rect.right > vw + 1);
        })
        .slice(0, 8)
        .map((el) => ({ tag: el.tagName, text: el.innerText?.slice(0, 80) ?? '', left: Math.round(el.getBoundingClientRect().left), right: Math.round(el.getBoundingClientRect().right) }));
    })()`),
  });

  console.log(JSON.stringify(results, null, 2));

  const failures = collectFailures(results);
  if (failures.length > 0) {
    console.error(`Smoke validation failed:\n${failures.map((failure) => `- ${failure}`).join("\n")}`);
    process.exitCode = 1;
  }
} finally {
  ws.close();
  chrome.kill();
}
