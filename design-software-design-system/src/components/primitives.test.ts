/**
 * Tests de `<sena-button>` y `<sena-field>` — E3 de HU-SHELL-006.
 */
import { beforeEach, describe, expect, it } from 'vitest';

import './button';
import './field';
import { normalizeVariant, VARIANTS } from './button';
import { cssTokens, localAliases, publishedTokens, shadowCss } from '../../test/component-helpers';

const mountButton = (attrs: Record<string, string> = {}, texto = 'Guardar') => {
  const el = document.createElement('sena-button');
  for (const [name, value] of Object.entries(attrs)) el.setAttribute(name, value);
  el.textContent = texto;
  document.body.append(el);
  return el;
};

const mountField = (attrs: Record<string, string> = {}, control = 'input') => {
  const el = document.createElement('sena-field');
  for (const [name, value] of Object.entries(attrs)) el.setAttribute(name, value);
  const inner = document.createElement(control) as HTMLInputElement;
  el.append(inner);
  document.body.append(el);
  return { el, control: inner };
};

describe('sena-button', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('se registra como custom element', () => {
    expect(customElements.get('sena-button')).toBeTypeOf('function');
  });

  it('expone las tres variantes que los MFEs duplicaban', () => {
    expect(VARIANTS).toEqual(['primary', 'secondary', 'danger']);
  });

  it.each(VARIANTS)('acepta la variante %s', (variant) => {
    const el = mountButton({ variant });
    expect(el.variant).toBe(variant);
    expect(el.getAttribute('data-variant')).toBe(variant);
  });

  it('cae en primary con una variante desconocida', () => {
    expect(normalizeVariant('raro')).toBe('primary');
  });

  it('envuelve un <button> nativo, no lo reemplaza', () => {
    const boton = mountButton({}).shadowRoot?.querySelector('button');
    expect(boton?.tagName).toBe('BUTTON');
    expect(boton?.getAttribute('type')).toBe('button');
  });

  it('el texto del consumidor va por slot', () => {
    const el = mountButton({}, 'Eliminar');
    expect(el.label).toBe('Eliminar');
    expect(el.shadowRoot?.querySelector('slot')).not.toBeNull();
  });

  it('propaga disabled al boton real', () => {
    const el = mountButton({ disabled: '' });
    expect(el.shadowRoot?.querySelector('button')?.hasAttribute('disabled')).toBe(true);
    expect(el.disabled).toBe(true);
  });

  it('permite alternar disabled desde la property', () => {
    const el = mountButton({});
    el.disabled = true;
    expect(el.hasAttribute('disabled')).toBe(true);
    el.disabled = false;
    expect(el.hasAttribute('disabled')).toBe(false);
  });

  it('respeta type=submit', () => {
    const el = mountButton({ type: 'submit' });
    expect(el.shadowRoot?.querySelector('button')?.getAttribute('type')).toBe('submit');
  });

  it('E4: solo usa tokens publicados', () => {
    const css = shadowCss(mountButton({}));
    const publicados = publishedTokens();
    const usados = cssTokens(css);
    expect(usados.length).toBeGreaterThan(0);
    for (const token of usados) {
      if (localAliases(css).has(token)) continue;
      expect(publicados.has(token), token + ' no esta publicado').toBe(true);
    }
  });

  it('E4: sin colores ni espaciados literales', () => {
    const css = shadowCss(mountButton({}));
    expect(css).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(css).not.toMatch(/\b(rgb|rgba|hsl|hsla)\(/);
    expect(css).not.toMatch(/:\s*[\d.]+(px|rem|em)\b/);
  });
});

describe('sena-field', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('se registra como custom element', () => {
    expect(customElements.get('sena-field')).toBeTypeOf('function');
  });

  it('enlaza la etiqueta con el control', () => {
    const { el, control } = mountField({ label: 'Correo', name: 'email' });
    const forAttr = el.shadowRoot?.querySelector('label')?.getAttribute('for');
    expect(forAttr).toBe(control.id);
    expect(control.id).not.toBe('');
  });

  it('el control vive en el light DOM, para no romper el binding', () => {
    const { el, control } = mountField({ label: 'Correo' });
    expect(el.contains(control)).toBe(true);
    expect(control.getRootNode()).toBe(document);
  });

  it('muestra help cuando no hay error', () => {
    const { el, control } = mountField({ label: 'Correo', help: 'Usalo institucional' });
    expect(el.shadowRoot?.querySelector('.help')?.textContent).toBe('Usalo institucional');
    expect(control.getAttribute('aria-describedby')).toContain('help');
  });

  it('el error tiene prioridad sobre la ayuda y marca invalido', () => {
    const { el, control } = mountField({
      label: 'Correo',
      help: 'Usalo institucional',
      error: 'Este campo es obligatorio',
    });
    expect(el.shadowRoot?.querySelector('.help')).toBeNull();
    expect(el.shadowRoot?.querySelector('.error')?.textContent).toBe('Este campo es obligatorio');
    expect(control.getAttribute('aria-invalid')).toBe('true');
    expect(el.getAttribute('data-invalid')).toBe('true');
  });

  it('el error se anuncia con role=alert', () => {
    const { el } = mountField({ label: 'X', error: 'Mal' });
    expect(el.shadowRoot?.querySelector('.error')?.getAttribute('role')).toBe('alert');
  });

  it('sin error, aria-invalid no aparece', () => {
    const { control } = mountField({ label: 'X' });
    expect(control.hasAttribute('aria-invalid')).toBe(false);
  });

  it('required propaga al control y marca la etiqueta', () => {
    const { el, control } = mountField({ label: 'Nombre', required: '' });
    expect(control.hasAttribute('required')).toBe(true);
    expect(el.shadowRoot?.querySelector('.required')?.textContent).toBe('*');
  });

  it.each(['input', 'select', 'textarea'])('acepta un <%s> como control', (tag) => {
    const { control } = mountField({ label: 'X' }, tag);
    expect(control.tagName).toBe(tag.toUpperCase());
  });

  it('limpia error al vaciarlo', () => {
    const { el } = mountField({ label: 'X' });
    el.setAttribute('error', '');
    el.removeAttribute('error');
    el.setAttribute('label', 'Y');
    expect(el.shadowRoot?.querySelector('.error')).toBeNull();
    expect(el.getAttribute('data-invalid')).toBe('false');
  });

  it('escapa label, help y error', () => {
    const { el } = mountField({ label: '<b>L</b>', help: '<i>H</i>', error: '<script>x</script>' });
    expect(el.shadowRoot?.querySelector('b')).toBeNull();
    expect(el.shadowRoot?.querySelector('i')).toBeNull();
    expect(el.shadowRoot?.querySelector('script')).toBeNull();
  });

  it('E4: sin colores ni espaciados literales', () => {
    const { el } = mountField({ label: 'X' });
    const css = Array.from(el.shadowRoot?.querySelectorAll('style') ?? [])
      .map((s) => s.textContent ?? '')
      .join('\n');
    expect(css).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    expect(css).not.toMatch(/\b(rgb|rgba|hsl|hsla)\(/);
    expect(css).not.toMatch(/:\s*[\d.]+(px|rem|em)\b/);
  });

  it('E4: solo usa tokens publicados', () => {
    const { el } = mountField({ label: 'X' });
    const css = shadowCss(el);
    const publicados = publishedTokens();

    // `--field-border*` son alias locales que el propio componente declara en
    // `:host` para poder cambiar el estado del campo. Se aceptan si ademas
    // resuelven a un token publicado.
    const locales = localAliases(css);
    for (const alias of locales) {
      const definicion = css.match(new RegExp(`${alias}\\s*:\\s*var\\((--[a-z0-9-]+)\\)`));
      expect(definicion, alias + ' no resuelve a un token').not.toBeNull();
      expect(publicados.has(definicion?.[1] ?? ''), alias + ' apunta a un token no publicado').toBe(
        true,
      );
    }

    const usados = cssTokens(css);
    expect(usados.length).toBeGreaterThan(0);
    for (const token of usados) {
      if (locales.has(token)) continue;
      expect(publicados.has(token), token + ' no esta publicado').toBe(true);
    }
  });
});