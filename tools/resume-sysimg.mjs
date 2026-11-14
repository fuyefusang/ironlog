import fs from "node:fs";
import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";

const url = "https://dl.google.com/android/repository/sys-img/android/x86_64-30_r11.zip";
const out = process.argv[2];
let have = 0;
try { have = fs.statSync(out).size; } catch {}

// Probe total size
const probe = await fetch(url, { method: "GET", headers: have ? { Range: `bytes=${have}-` } : {} });
console.log("status", probe.status, "content-range", probe.headers.get("content-range"), "len", probe.headers.get("content-length"));

if (probe.status === 206) {
  const m = /\/(\d+)$/.exec(probe.headers.get("content-range") || "");
  const total = m ? Number(m[1]) : 0;
  console.log(`resume from ${(have / 1048576).toFixed(0)} / ${(total / 1048576).toFixed(0)} MB`);
  let done = have, lastLog = 0;
  const src = Readable.fromWeb(probe.body);
  src.on("data", (c) => {
    done += c.length;
    const now = Date.now();
    if (now - lastLog > 5000) { lastLog = now; console.log(`${(done / 1048576).toFixed(0)} / ${(total / 1048576).toFixed(0)} MB`); }
  });
  await pipeline(src, fs.createWriteStream(out, { flags: "a" }));
  console.log("DONE", fs.statSync(out).size, "expected", total);
} else {
  console.log("server did not honor range; full download");
  let done = 0, lastLog = 0;
  const total = Number(probe.headers.get("content-length") || 0);
  const src = Readable.fromWeb(probe.body);
  src.on("data", (c) => {
    done += c.length;
    const now = Date.now();
    if (now - lastLog > 5000) { lastLog = now; console.log(`${(done / 1048576).toFixed(0)} / ${(total / 1048576).toFixed(0)} MB`); }
  });
  await pipeline(src, fs.createWriteStream(out));
  console.log("DONE", fs.statSync(out).size);
}
