import fs from "node:fs";
import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";

const url = "https://dl.google.com/android/repository/sys-img/android/x86_64-30_r11.zip";
const out = process.argv[2];
const res = await fetch(url);
if (!res.ok) { console.error("HTTP " + res.status); process.exit(1); }
const total = Number(res.headers.get("content-length") || 0);
let done = 0, lastLog = 0;
const src = Readable.fromWeb(res.body);
const ws = fs.createWriteStream(out);
src.on("data", (c) => {
  done += c.length;
  const now = Date.now();
  if (now - lastLog > 5000) {
    lastLog = now;
    console.log(`${(done/1048576).toFixed(0)} / ${(total/1048576).toFixed(0)} MB`);
  }
});
await pipeline(src, ws);
console.log("DONE " + out + " " + fs.statSync(out).size);
