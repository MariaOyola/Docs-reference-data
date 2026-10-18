import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

import { buildArtifacts, parseCanonicalCss, parseSpec } from '../../scripts/generate-tokens.mjs';
import { tokens } from './tokens.generated';
import { color, businessState, space, radius, shadow, typography, zIndex } from './tokens';

const HERE = dirname(fileURLToPath(import.meta.url));

describe('E1 tokens migrados desde el spec', () => {
  it('el spec esta disponible como fuente canonica', () => {
    // Local: repo hermano. CI: SENA_TOKEN_SPEC_DIR. buildArtifacts() lanza si no encuentra.
    const artifacts = buildArtifacts();

    expect(existsSync(artifacts.specPath)).toBe(true);
    expect(existsSync(artifacts.cssPath)).toBe(true);
    expect(artifacts.specPath).toContain('design-tokens.md');
    expect(artifacts.cssPath).toContain('tokens.css');
  });

  it('los tokens coinciden 1:1 con el spec y su css canonico', () => {
    const artifacts = buildArtifacts();
    const byKey = new Map<string, string>();
    for (const token of [...artifacts.specTokens, ...artifacts.cssTokens]) {
      if (!byKey.has(token.key)) byKey.set(token.key, token.value);
    }

    // El css canonico manda sobre el subconjunto del spec (asi lo declara el doc).
    for (const [key, value] of byKey) {
      const actual = (tokens as Record<string, string>)[key];
      expect(actual, `falta el token ${key}`).toBeDefined();
      expect(actual, `${key} diverge`).toBe(value);
    }
  });

  it('no deja placeholders vacios', () => {
    const vacios = Object.entries(tokens).filter(([, value]) => value.trim().length === 0);
    expect(vacios).toEqual([]);
  });

  it('los grupos del spec estan completos', () => {
    const requeridos = [
      'color_brand',
      'color_text',
      'color_text_muted',
      'color_border',
      'color_danger',
      'color_warning',
      'color_success',
      'color_info',
      'font_family_base',
      'font_size_md',
      'space_1',
      'space_8',
      'radius_sm',
      'radius_lg',
      'shadow_sm',
      'z_dropdown',
    ];
    for (const key of requeridos) {
      expect(tokens, `falta ${key}`).toHaveProperty(key);
    }
  });
});

describe('E3 tipado estricto', () => {
  it('tokens expone claves, no un Record generico', () => {
    // Esta asercion solo compila si TokenKey es un union de literales.
    const clave: keyof typeof tokens = 'color_brand';
    expect(tokens[clave]).toBe('#007832');
  });

  it('los grupos exponen los valores del spec', () => {
    expect(color.brand).toBe('#007832');
    expect(color.textMuted).toBe('#475467');
    expect(color.border).toBe('#eaecf0');
    expect(space[4]).toBe('16px');
    expect(radius.md).toBe('10px');
    expect(zIndex.toast).toBe('1030');
  });
});

describe('Estados de negocio (spec: color + icono + texto, nunca solo color)', () => {
  it('cubre los cuatro estados del catalogo', () => {
    expect(Object.keys(businessState)).toEqual([
      'BORRADOR',
      'EN_REVISION',
      'PUBLICADO',
      'CONFLICTO',
    ]);
  });

  it('CONFLICTO usa color-conflict sobre danger-surface', () => {
    expect(businessState.CONFLICTO).toEqual({
      color: '#b42318',
      surface: '#fef3f2',
    });
  });

  it('PUBLICADO y EN_REVISION tienen superficies distintas', () => {
    expect(businessState.PUBLICADO.surface).not.toBe(businessState.EN_REVISION.surface);
  });
});

describe('DoD: test que falla si tokens divergen de la fuente', () => {
  it('los archivos generados estan al dia con el generador', () => {
    const artifacts = buildArtifacts();
    const tsPath = join(HERE, 'tokens.generated.ts');
    const cssPath = join(HERE, 'tokens.generated.css');

    expect(readFileSync(tsPath, 'utf8'), 'tokens.generated.ts desactualizado, corre npm run generate:tokens').toBe(
      artifacts.ts,
    );
    expect(readFileSync(cssPath, 'utf8'), 'tokens.generated.css desactualizado, corre npm run generate:tokens').toBe(
      artifacts.css,
    );
  });

  it('el parser ignora filas de resumen que no son tokens', () => {
    // "regular 400 · medium 500 · bold 700" describe un grupo, no un token.
    const parsed = parseSpec('| `font-weight` | `regular 400 · medium 500 · bold 700` |');
    expect(parsed).toEqual([]);

    const css = parseCanonicalCss(':root {\n  --shadow-sm: 0 1px 2px rgba(0,0,0,.06);\n}\n');
    expect(css).toHaveLength(1);
  });

  it('la escala de espaciado del spec es la base 4px', () => {
    expect(space[1]).toBe('4px');
    expect(space[2]).toBe('8px');
    expect(space[3]).toBe('12px');
    expect(space[4]).toBe('16px');
    expect(space[5]).toBe('24px');
    expect(space[6]).toBe('32px');
    expect(space[7]).toBe('40px');
    expect(space[8]).toBe('48px');
  });

  it('la tipografia cubre la escala del spec', () => {
    expect(typography.sizeXs).toBe('12px');
    expect(typography.sizeMd).toBe('16px');
    expect(typography.size3xl).toBe('30px');
    expect(typography.weightBold).toBe('700');
    expect(typography.lineHeightRelaxed).toBe('1.7');
  });

  it('las sombras y el layout vienen del css canonico', () => {
    expect(shadow.sm).toContain('rgba(16, 24, 40');
    expect(tokens.topbar_height).toBe('72px');
    expect(tokens.sidebar_width).toBe('264px');
  });
});