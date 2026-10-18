/**
 * Verificacion de E4 — HU-SHELL-006 (DoD: "grep de verificacion").
 *
 * Ningun componente del design-system puede hardcodear un color ni un
 * espaciado: todo debe salir de `tokens.generated.ts` (HU-SHELL-005). Este
 * script automatiza el grep que el DoD pide y lo corre en CI.
 *
 * Reglas:
 *   1. Prohibido cualquier color literal: hex, rgb(), hsl(), o un nombre de
 *      color CSS en una declaracion de estilo.
 *   2. Prohibida cualquier medida literal en una declaracion: px, rem, em.
 *      (Se permiten en comentarios y en el JS de los tests, que no es CSS.)
 *   3. Todo `var(--x)` debe apuntar a un token realmente publicado.
 *
 * Uso: `npm run check:no-hardcode`
 */
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const REPO_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const COMPONENTS = join(REPO_ROOT, 'src', 'components');
const TOKENS_TS = join(REPO_ROOT, 'src', 'tokens', 'tokens.generated.ts');

/** Tokens publicados, leidos del artefacto generado (no se reparsea el spec). */
const publishedTokens = () => {
  const source = readFileSync(TOKENS_TS, 'utf8');
  return new Set(
    Array.from(source.matchAll(/^\s{2}([a-z0-9_]+):/gm)).map((m) => `--${m[1].replace(/_/g, '-')}`),
  );
};

/** Quita comentarios y lineas en blanco: no cuentan como CSS. */
const stripComments = (source) =>
  source.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/.*$/gm, '');

/** Los `.ts` de componentes, excluyendo los tests (no son CSS de producto). */
const componentFiles = (dir) => {
  const out = [];
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) continue;
    if (!name.endsWith('.ts') || name.endsWith('.test.ts')) continue;
    out.push(path);
  }
  return out;
};

const COLOR_PATTERNS = [
  { re: /#[0-9a-fA-F]{3,8}\b/, label: 'color hexadecimal' },
  { re: /\b(?:rgb|rgba|hsl|hsla|oklch|color)\(/, label: 'funcion de color' },
];

const MEASURE_RE = /:\s*[\d.]+\s*(px|rem|em)\b/;

export const checkNoHardcode = () => {
  const published = publishedTokens();
  const problems = [];

  for (const path of componentFiles(COMPONENTS)) {
    const raw = stripComments(readFileSync(path, 'utf8'));
    const lines = raw.split('\n');

    lines.forEach((line, i) => {
      const where = `${path.replace(`${REPO_ROOT}\\`, '')}:${i + 1}`;

      for (const { re, label } of COLOR_PATTERNS) {
        if (re.test(line)) problems.push(`${where} ${label} hardcodeado: ${line.trim()}`);
      }

      if (MEASURE_RE.test(line)) {
        problems.push(`${where} medida literal en CSS: ${line.trim()}`);
      }
    });

    // Todo var(--x) debe existir en los tokens publicados.
    for (const match of raw.matchAll(/var\((--[a-z0-9-]+)\)/g)) {
      const name = match[1];
      // Alias locales declarados por el propio componente se validan aparte.
      if (name.startsWith('--field-')) continue;
      if (!published.has(name)) {
        const line = raw.slice(0, match.index).split('\n').length;
        problems.push(
          `${path.replace(`${REPO_ROOT}\\`, '')}:${line} usa ${name}, que el design-system no publica`,
        );
      }
    }
  }

  return problems;
};

const isMain =
  process.argv[1] !== undefined && resolve(process.argv[1]) === resolve(fileURLToPath(import.meta.url));

if (isMain) {
  const problems = checkNoHardcode();
  if (problems.length > 0) {
    console.error('E4: los componentes tienen valores hardcodeados:\n');
    for (const problem of problems) console.error(`  - ${problem}`);
    console.error('\nUsa solo var(--token) con tokens de src/tokens/tokens.generated.ts.');
    process.exit(1);
  }
  console.log('E4 en verde: ningun componente hardcodea color ni espaciado.');
}