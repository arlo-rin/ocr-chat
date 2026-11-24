#!/usr/bin/env node
/**
 * Local OpenAI-compatible server for development and UI testing.
 * Never use this in production: it accepts any API key.
 *
 *   node tools/mock-ai-server.js
 *   Android emulator endpoint: http://10.0.2.2:8787/v1/chat/completions
 *   Demo chat page: http://10.0.2.2:8787/demo
 */
const http = require("http");

const PORT = Number(process.env.PORT || 8787);
const HOST = "0.0.0.0";

const replyPayload = {
  replies: [
    { style: "自然", text: "是有点忙，不过周六晚上可以留给你。" },
    { style: "稳妥", text: "最近事情多一点，周六我应该有空，你定时间吧。" },
    { style: "轻松", text: "被你发现了，忙到快忘记吃饭。周六求投喂。" },
  ],
};

const demoHtml = `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
  <title>聊天演示 · 侧答</title>
  <style>
    * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
    html, body { height: 100%; margin: 0; }
    body {
      background: #ededed; color: #191919;
      font-family: -apple-system, "PingFang SC", "Noto Sans CJK SC", "Microsoft YaHei", sans-serif;
      display: flex; flex-direction: column; height: 100vh;
    }
    .top {
      background: #ededed; padding: 14px 16px 12px;
      display: flex; align-items: center; gap: 12px;
      border-bottom: 1px solid #d8d8d8; flex: 0 0 auto;
    }
    .back { font-size: 27px; font-weight: 300; line-height: 1; color: #191919; }
    .person { flex: 1; }
    .name { font-size: 17px; font-weight: 600; letter-spacing: .02em; }
    .status { font-size: 11px; color: #8c8c8c; margin-top: 3px; }
    .more { font-size: 22px; letter-spacing: 2px; color: #333; }
    .chat {
      flex: 1; overflow-y: auto; padding: 18px 16px 24px;
      display: flex; flex-direction: column; gap: 14px;
    }
    .stamp { text-align: center; font-size: 11px; color: #9d9d9d; margin: 2px 0 4px; }
    .row { display: flex; align-items: flex-start; gap: 9px; }
    .row.right { flex-direction: row-reverse; }
    .avatar {
      width: 38px; height: 38px; border-radius: 7px; flex: 0 0 auto;
      display: grid; place-items: center; font-size: 15px; font-weight: 700;
      color: #fff; background: linear-gradient(145deg, #efb58d, #d4895e);
    }
    .row.right .avatar { background: linear-gradient(145deg, #0f8d79, #076654); }
    .bubble {
      position: relative; max-width: 73%; padding: 11px 13px;
      font-size: 15px; line-height: 1.55; border-radius: 6px;
      background: #fff; box-shadow: 0 1px 0 rgba(0,0,0,.03);
      white-space: pre-wrap;
    }
    .row.right .bubble { background: #95ec69; }
    .bubble::before {
      content: ""; position: absolute; left: -5px; top: 14px;
      border-top: 6px solid transparent; border-bottom: 6px solid transparent;
      border-right: 6px solid #fff;
    }
    .row.right .bubble::before {
      left: auto; right: -5px; border-right: 0; border-left: 6px solid #95ec69;
    }
    .composer {
      flex: 0 0 auto; background: #f7f7f7; border-top: 1px solid #d8d8d8;
      padding: 9px 10px 12px; display: flex; align-items: center; gap: 9px;
    }
    .icon { font-size: 23px; color: #555; line-height: 1; }
    .input {
      flex: 1; height: 39px; border-radius: 5px; background: #fff;
      padding: 10px 11px; font-size: 14px; color: #b1b1b1;
    }
    @media (min-width: 640px) {
      body { max-width: 520px; margin: 0 auto; box-shadow: 0 0 40px rgba(0,0,0,.12); }
    }
  </style>
</head>
<body>
  <div class="top">
    <div class="back">‹</div>
    <div class="person">
      <div class="name">林小满</div>
      <div class="status">对方正在输入…</div>
    </div>
    <div class="more">•••</div>
  </div>
  <div class="chat">
    <div class="stamp">今天 18:42</div>
    <div class="row">
      <div class="avatar">满</div>
      <div class="bubble">周六晚上有空吗？想约你吃个饭 😊</div>
    </div>
    <div class="row right">
      <div class="avatar">我</div>
      <div class="bubble">我看看安排，最近有点忙</div>
    </div>
    <div class="row">
      <div class="avatar">满</div>
      <div class="bubble">那家新开的云南菜，评价好像不错。要是你有时间，我们可以去试试。</div>
    </div>
    <div class="row">
      <div class="avatar">满</div>
      <div class="bubble">你最近是不是很忙呀？别太累啦。</div>
    </div>
  </div>
  <div class="composer">
    <div class="icon">☺</div>
    <div class="input">输入消息…</div>
    <div class="icon">＋</div>
  </div>
</body>
</html>`;

function sendJson(res, status, value) {
  const body = Buffer.from(JSON.stringify(value), "utf8");
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": body.length,
    "Cache-Control": "no-store",
  });
  res.end(body);
}

function completion(content) {
  return {
    id: "chatcmpl-side-reply-mock",
    object: "chat.completion",
    created: Math.floor(Date.now() / 1000),
    model: "mock-chat",
    choices: [{
      index: 0,
      message: { role: "assistant", content },
      finish_reason: "stop",
    }],
    usage: { prompt_tokens: 0, completion_tokens: 0, total_tokens: 0 },
  };
}

/** The app sends `content` as an array of text/image_url parts; older clients send a string. */
function textOf(content) {
  if (typeof content === "string") return content;
  if (!Array.isArray(content)) return "";
  return content
    .filter((part) => part && typeof part === "object" && typeof part.text === "string")
    .map((part) => part.text)
    .join("\n");
}

/** Approximate size of the inline JPEG, for logging only. */
function imageBytesOf(messages) {
  let bytes = 0;
  for (const message of messages) {
    if (!message || !Array.isArray(message.content)) continue;
    for (const part of message.content) {
      const url = part && part.image_url && part.image_url.url;
      if (typeof url === "string" && url.startsWith("data:image/")) {
        bytes += Math.floor((url.length - url.indexOf(",") - 1) * 0.75);
      }
    }
  }
  return bytes;
}

const server = http.createServer((req, res) => {
  if (req.method === "GET" && req.url === "/health") {
    sendJson(res, 200, { ok: true, service: "side-reply-mock" });
    return;
  }

  if (req.method === "GET" && (req.url === "/demo" || req.url === "/demo/")) {
    const html = Buffer.from(demoHtml, "utf8");
    res.writeHead(200, {
      "Content-Type": "text/html; charset=utf-8",
      "Content-Length": html.length,
      "Cache-Control": "no-store",
    });
    res.end(html);
    return;
  }

  if (req.method === "POST" && req.url.startsWith("/v1/chat/completions")) {
    const chunks = [];
    let size = 0;
    req.on("data", (chunk) => {
      size += chunk.length;
      // A chat screenshot as base64 JPEG is a few hundred KB; keep generous headroom.
      if (size > 4 * 1024 * 1024) {
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on("end", () => {
      let body;
      try {
        body = JSON.parse(Buffer.concat(chunks).toString("utf8"));
      } catch {
        sendJson(res, 400, { error: { message: "invalid JSON" } });
        return;
      }

      const messages = Array.isArray(body.messages) ? body.messages : [];
      const userText = messages
        .filter((message) => message && message.role === "user")
        .map((message) => textOf(message.content))
        .join("\n");
      const imageBytes = imageBytesOf(messages);
      const isConnectivityTest = userText.includes("连接测试");
      const content = isConnectivityTest
          ? "正常"
          : JSON.stringify(replyPayload);
      const model = body.model || "mock-chat";

      console.log(
        `[mock] ${isConnectivityTest ? "test" : "suggest"} ` +
        `model=${model} promptChars=${userText.length} imageKB=${Math.round(imageBytes / 1024)}`,
      );
      setTimeout(() => {
        try {
          sendJson(res, 200, completion(content));
        } catch {
          // Client went away (the reply sheet was closed). Nothing to do.
        }
      }, 650);
    });
    return;
  }

  sendJson(res, 404, { error: { message: "not found" } });
});

server.listen(PORT, HOST, () => {
  console.log(`SideReply mock AI:  http://${HOST}:${PORT}/v1/chat/completions`);
  console.log(`Demo chat page:     http://localhost:${PORT}/demo`);
  console.log("Press Ctrl+C to stop.");
});
