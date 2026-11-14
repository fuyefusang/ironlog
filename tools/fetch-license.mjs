import fs from "node:fs";

const out = process.argv[2];
const want = process.argv[3] || "Apache-2.0";
const sources = [
  `https://raw.githubusercontent.com/spdx/license-list-data/main/text/${want}.txt`,
  `https://www.apache.org/licenses/LICENSE-2.0.txt`,
];
const ua = "IronLog-build/1.0 (license fetch)";
const marker = want === "MIT" ? "MIT License" : "Apache License";

for (const url of sources) {
  try {
    const res = await fetch(url, { headers: { "User-Agent": ua, Accept: "text/plain" } });
    if (!res.ok) {
      console.error(`${url} -> HTTP ${res.status}`);
      continue;
    }
    const text = await res.text();
    if (!text.includes(marker) || text.length < 3000) {
      console.error(`${url} -> unexpected content (${text.length} chars)`);
      continue;
    }
    fs.writeFileSync(out, text, "utf8");
    console.log(`wrote ${out} from ${url} (${text.length} chars)`);
    process.exit(0);
  } catch (e) {
    console.error(`${url} -> ${e.message}`);
  }
}
process.exit(1);
