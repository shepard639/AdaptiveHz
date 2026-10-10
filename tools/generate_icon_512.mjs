import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const sharp = require('f:/Antigravity/healthcraft/node_modules/sharp');
import fs from 'fs';
import path from 'path';

const svg = `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <defs>
    <!-- Background Gradient (Ceramic White to Pale Sage) -->
    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFFFFF" />
      <stop offset="60%" stop-color="#FBFDFB" />
      <stop offset="100%" stop-color="#F0F6F2" />
    </linearGradient>

    <!-- Leaf Emerald Gradient -->
    <linearGradient id="leafGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0FA86E" />
      <stop offset="45%" stop-color="#12A170" />
      <stop offset="100%" stop-color="#34D399" />
    </linearGradient>

    <!-- Ambient Shadow for Leaf -->
    <filter id="leafShadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="16" stdDeviation="20" flood-color="#0FA86E" flood-opacity="0.25"/>
      <feDropShadow dx="0" dy="6" stdDeviation="8" flood-color="#0D5F3B" flood-opacity="0.18"/>
    </filter>

    <!-- Pulse Glow Filter -->
    <filter id="pulseGlow" x="-30%" y="-30%" width="160%" height="160%">
      <feDropShadow dx="0" dy="0" stdDeviation="4" flood-color="#FFFFFF" flood-opacity="0.6"/>
    </filter>
  </defs>

  <!-- App Icon Base Background: Rounded Squircle Card -->
  <rect x="16" y="16" width="480" height="480" rx="112" fill="url(#bgGrad)" stroke="#E2EBE5" stroke-width="4"/>

  <!-- Emerald Organic Leaf & Pulse Emblem -->
  <g filter="url(#leafShadow)">
    <!-- Base Leaf Body -->
    <path d="M 256 95 C 335 95 400 170 400 268 C 400 366 335 428 256 428 C 177 428 112 366 112 268 C 112 170 177 95 256 95 Z"
          fill="url(#leafGrad)"/>

    <!-- Inner Organic Light Reflection Accent -->
    <path d="M 256 108 C 322 108 378 175 378 258 C 378 332 328 388 262 394 C 273 344 301 276 334 232 C 306 254 273 293 256 338 C 256 253 256 179 256 108 Z"
          fill="#FFFFFF" opacity="0.16"/>
  </g>

  <!-- Dynamic Hz Frequency Pulse Wave (Crisp White with Subtle Glow) -->
  <g filter="url(#pulseGlow)">
    <path d="M 148 268 L 195 268 L 223 194 L 251 342 L 279 160 L 307 296 L 330 268 L 364 268"
          fill="none"
          stroke="#FFFFFF"
          stroke-width="18"
          stroke-linecap="round"
          stroke-linejoin="round"/>

    <!-- Pulse Apex Energy Spark Dot -->
    <circle cx="279" cy="160" r="10" fill="#FFFFFF"/>
    <circle cx="279" cy="160" r="16" fill="#FFFFFF" opacity="0.3"/>
  </g>
</svg>
`;

async function generate() {
  const targetDir = 'f:/Antigravity/Max_hz/store_assets';
  if (!fs.existsSync(targetDir)) {
    fs.mkdirSync(targetDir, { recursive: true });
  }

  const outPath = path.join(targetDir, 'icon_512.png');
  await sharp(Buffer.from(svg))
    .resize(512, 512)
    .png()
    .toFile(outPath);

  console.log('Successfully generated 512x512 icon at:', outPath);
}

generate().catch(err => {
  console.error(err);
  process.exit(1);
});
