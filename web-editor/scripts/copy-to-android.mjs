import { cpSync, existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const projectRoot = join(__dirname, '..');
const distDir = join(projectRoot, 'dist');
const assetsDir = join(projectRoot, '..', 'app', 'src', 'main', 'assets', 'editor');

if (!existsSync(join(distDir, 'index.html'))) {
  console.error('Missing dist/index.html. Run `npm run build` first.');
  process.exit(1);
}

rmSync(assetsDir, { recursive: true, force: true });
mkdirSync(assetsDir, { recursive: true });
cpSync(distDir, assetsDir, { recursive: true });

// Inlined scripts don't need CORS mode; drop the attribute Vite adds.
const indexPath = join(assetsDir, 'index.html');
writeFileSync(indexPath, readFileSync(indexPath, 'utf8').replaceAll(' crossorigin', ''), 'utf8');

console.log(`Copied ${distDir} -> ${assetsDir}`);
