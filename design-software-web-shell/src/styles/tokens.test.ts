import { readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

const HERE = dirname(fileURLToPath(import.meta.url));
const SHELL_TOKENS = resolve(HERE, '../styles/tokens.css');
const DESIGN_SYSTEM_CSS = resolve(HERE, '../../../design-software-design-system/src/tokens/tokens.generated.css');

const read = (path: string): string => readFileSync(path, 'utf8');

/** Extrae `--nombre: valor` de un bloque, resolviendo solo declaraciones directas. */
const declarationsOf = (css: string): Map<string, string> => {
  const found = new Map<string, string>();
  for (const line of css.split(/\r?\n/)) {
    const match = line.match(/^\s*(--[a-z0-9-]+)\s*:\s*([^;]+);/);
    if (match === null) continue;
    const name = match[1] ?? '';
    const value = (match[2] ?? '').trim();
    // Una declaracion que apunta a si misma es un ciclo CSS.
    if (value === `var(${name})`) continue;
    found.set(name, value);
  }
  return found;
};

describe('E2 el shell no redefine los tokens del design-system', () => {
  const spec = declarationsOf(read(DESIGN_SYSTEM_CSS));
  const shell = declarationsOf(read(SHELL_TOKENS));

  it('el shell importa los tokens canonicos', () => {
    expect(read(SHELL_TOKENS)).toContain("@import '@sena/design-system/tokens.css'");
  });

  it('no redefine con un valor literal ningun token del spec', () => {
    const offenders: string[] = [];

    for (const [name, value] of shell) {
      // Solo las declaraciones del shell (las del @import no aparecen aqui).
      if (!spec.has(name)) continue;
      if (value.startsWith('var(')) continue;
      offenders.push(`${name}: ${value}`);
    }

    expect(offenders, `el shell redefine tokens del spec: ${offenders.join(', ')}`).toEqual([]);
  });

  it('no se auto-referencia (seria un ciclo CSS)', () => {
    const cycles: string[] = [];
    for (const [name, value] of shell) {
      if (value === `var(${name})`) cycles.push(name);
    }
    expect(cycles).toEqual([]);
  });

  it('los alias del shell apuntan al token canonico correcto', () => {
    expect(shell.get('--color-danger-soft')).toBe('var(--color-danger-surface)');
    expect(spec.get('--color-danger-surface')).toBe('#fef3f2');
  });
});

describe('Los tokens propios del shell no colisionan con el spec', () => {
  const spec = declarationsOf(read(DESIGN_SYSTEM_CSS));

  it('los tokens propios usan prefijo shell-', () => {
    const shellOwn = read(join(HERE, '../styles/shell-tokens.css'));
    const names = [...shellOwn.matchAll(/^\s*(--[a-z0-9-]+)\s*:/gm)].map((m) => m[1] ?? '');

    expect(names.length).toBeGreaterThan(0);
    const sinPrefijo = names.filter((name) => !name.startsWith('--shell-'));
    expect(sinPrefijo, `tokens propios sin prefijo shell-: ${sinPrefijo.join(', ')}`).toEqual([]);
  });

  it('ningun token propio repite un nombre del spec', () => {
    const shellOwn = read(join(HERE, '../styles/shell-tokens.css'));
    const names = [...shellOwn.matchAll(/^\s*(--[a-z0-9-]+)\s*:/gm)].map((m) => m[1] ?? '');

    const colisiones = names.filter((name) => spec.has(name));
    expect(colisiones, `colisionan con el spec: ${colisiones.join(', ')}`).toEqual([]);
  });
});

describe('E1 los valores que el shell consume coinciden con el spec', () => {
  const spec = declarationsOf(read(DESIGN_SYSTEM_CSS));

  it('los colores semanticos coinciden 1:1', () => {
    // Valores que el shell ya consumia y ahora vienen del design-system.
    expect(spec.get('--color-brand')).toBe('#007832');
    expect(spec.get('--color-text')).toBe('#101828');
    expect(spec.get('--color-surface')).toBe('#ffffff');
    expect(spec.get('--color-warning')).toBe('#93370d');
    expect(spec.get('--color-success')).toBe('#067647');
    expect(spec.get('--color-info')).toBe('#175cd3');
  });

  it('los tokens que el shell habia divergido ahora vienen del spec', () => {
    // Antes: #667085 / #e4e7ec. El spec dice #475467 / #eaecf0.
    expect(spec.get('--color-text-muted')).toBe('#475467');
    expect(spec.get('--color-border')).toBe('#eaecf0');
  });

  it('la escala de espaciado del spec es la base 4px', () => {
    expect(spec.get('--space-1')).toBe('4px');
    expect(spec.get('--space-3')).toBe('12px');
    expect(spec.get('--space-8')).toBe('48px');
  });
});
