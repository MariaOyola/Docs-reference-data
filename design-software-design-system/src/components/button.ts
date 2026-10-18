/**
 * `<sena-button>` — E3 de HU-SHELL-006.
 *
 * Boton base. El inventario encontro que los MFEs no duplican componentes React
 * de boton, sino **clases CSS**: `.primaryButton` / `.secondaryButton` /
 * `.dangerButton` en `academic-management-mfe`, mas 8 juegos mas en
 * `document-mfe`, `reference-data-mfe` y `monitoring-mfe`. Este componente es el
 * unico lugar donde eso deberia vivir.
 *
 * Uso:
 *
 *   <sena-button variant="primary">Guardar</sena-button>
 *   <sena-button variant="danger" disabled>Eliminar</sena-button>
 *
 * Contrato de atributos:
 *   - `variant`  `primary` (default) | `secondary` | `danger`.
 *   - `disabled` Booleano. Reflejado a `aria-disabled`.
 *   - `type`     `button` (default) | `submit` | `reset`.
 *
 * Accesible: los tres estados que los MFEs duplicaban a mano quedan aqui —
 * `:hover`, `:disabled` y `:focus-visible` — con area tactil de 44px.
 *
 * NOTA de alcance: `<button>` nativo ya ofrece casi todo esto. Este componente
 * existe para **unificar el token de color** entre MFEs, no para reimplementar
 * el elemento. Por eso envuelve un `<button>` real en vez de sustituirlo.
 */
import { SenaElement, define } from './base-element';

const TAG = 'sena-button';

export const VARIANTS = ['primary', 'secondary', 'danger'] as const;
export type Variant = (typeof VARIANTS)[number];

export const normalizeVariant = (value: string | null | undefined): Variant =>
  VARIANTS.includes(value as Variant) ? (value as Variant) : 'primary';

export class SenaButton extends SenaElement {
  static get observedAttributes(): string[] {
    return ['variant', 'disabled', 'type'];
  }

  get variant(): Variant {
    return normalizeVariant(this.attr('variant', 'primary'));
  }

  set variant(value: Variant) {
    this.setAttribute('variant', value);
  }

  get disabled(): boolean {
    return this.hasAttribute('disabled');
  }

  set disabled(value: boolean) {
    if (value) this.setAttribute('disabled', '');
    else this.removeAttribute('disabled');
  }

  /** Contenido provisto por el consumidor via slot. */
  get label(): string {
    return this.textContent ?? '';
  }

  connectedCallback(): void {
    this.update();
  }

  attributeChangedCallback(): void {
    if (this.isConnected) this.update();
  }

  protected attributeChangedCallbackFor(): void {
    /* no-op */
  }

  protected render(): string {
    const variant = this.variant;
    const type = this.attr('type', 'button');
    const disabled = this.disabled;

    this.setAttribute('data-variant', variant);

    return `
      <button
        class="button"
        part="button"
        type="${this.escape(type)}"
        ${disabled ? 'disabled' : ''}
      ><slot></slot></button>
    `;
  }

  protected styles(): string {
    return `
      :host { display: inline-block; font-family: var(--font-family-base); }
      :host([hidden]) { display: none; }

      .button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: var(--space-2);
        min-block-size: var(--target-touch-min);
        padding: var(--space-2) var(--space-5);
        border: var(--space-1) solid transparent;
        border-radius: var(--radius-md);
        font: inherit;
        font-weight: var(--font-weight-medium);
        line-height: var(--line-height-normal);
        cursor: pointer;
        /* Sin token de transicion en el spec: se usa una duracion corta fija y
           prefers-reduced-motion la anula abajo. Asi el componente no depende
           de un token que el design-system todavia no publica. */
        transition: background-color 150ms, border-color 150ms;
      }

      /* primary */
      :host([data-variant='primary']) .button {
        background: var(--color-brand);
        border-color: var(--color-brand);
        color: var(--color-background);
      }
      :host([data-variant='primary']) .button:hover:not(:disabled) {
        background: var(--color-brand-hover);
        border-color: var(--color-brand-hover);
      }
      :host([data-variant='primary']) .button:active:not(:disabled) {
        background: var(--color-brand-strong);
        border-color: var(--color-brand-strong);
      }

      /* secondary */
      :host([data-variant='secondary']) .button {
        background: transparent;
        border-color: var(--color-border);
        color: var(--color-text);
      }
      :host([data-variant='secondary']) .button:hover:not(:disabled) {
        background: var(--color-surface-muted);
        border-color: var(--color-text-muted);
      }

      /* danger */
      :host([data-variant='danger']) .button {
        background: var(--color-danger);
        border-color: var(--color-danger);
        color: var(--color-background);
      }
      :host([data-variant='danger']) .button:hover:not(:disabled) {
        background: var(--color-danger);
        border-color: var(--color-danger);
        filter: brightness(0.92);
      }

      .button:focus-visible {
        outline: var(--space-1) solid var(--color-info);
        outline-offset: var(--space-1);
      }

      .button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      @media (prefers-reduced-motion: reduce) {
        .button { transition: none; }
      }
    `;
  }
}

define(TAG, SenaButton);

export const register = (): void => {
  define(TAG, SenaButton);
};

declare global {
  interface HTMLElementTagNameMap {
    'sena-button': SenaButton;
  }
}