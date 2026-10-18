/**
 * `<sena-field>` — E3 de HU-SHELL-006.
 *
 * Campo de formulario (input, select, textarea) con etiqueta, ayuda y error.
 * El inventario encontro `.field` / `.fieldError` duplicados en
 * `academic-management-mfe` (11 usos), `document-mfe` (3 copias),
 * `monitoring-mfe` (18 usos) y `reference-data-mfe`.
 *
 * Uso:
 *
 *   <sena-field label="Correo" name="email" required>
 *     <input type="email" />
 *   </sena-field>
 *
 *   <sena-field label="Estado" error="Este campo es obligatorio">
 *     <select><option>...</option></select>
 *   </sena-field>
 *
 * El control real va en el **light DOM** dentro de `<sena-field>`, no en el
 * shadow: asi se conserva el binding de React y el `name` real del formulario
 * sin necesidad de proxies. El componente solo aporta el marco, la etiqueta, la
 * ayuda y el error.
 *
 * Contrato de atributos:
 *   - `label`      Requerido. Texto de la etiqueta.
 *   - `help`       Opcional. Texto de ayuda.
 *   - `error`      Opcional. Mensaje de error. Su presencia marca el campo como invalido.
 *   - `required`   Booleano. Muestra el asterisco y `aria-required`.
 *   - `id`         Base de los ids. Por defecto se deriva de `name` o del orden.
 *
 * Accesible: `aria-describedby` apunta a la ayuda o al error segun corresponda,
 * y `aria-invalid` se activa solo cuando hay error.
 */
import { SenaElement, define } from './base-element';

const TAG = 'sena-field';

let counter = 0;

export class SenaField extends SenaElement {
  static get observedAttributes(): string[] {
    return ['label', 'help', 'error', 'required'];
  }

  get label(): string {
    return this.attr('label');
  }

  set label(value: string) {
    this.setAttribute('label', value);
  }

  get help(): string {
    return this.attr('help');
  }

  get error(): string {
    return this.attr('error');
  }

  set error(value: string) {
    if (value === '') this.removeAttribute('error');
    else this.setAttribute('error', value);
  }

  get required(): boolean {
    return this.hasAttribute('required');
  }

  /** Id estable del campo, para enlazar label y control. */
  get fieldId(): string {
    return this.attr('id') || this.attr('name') || `sena-field-${(counter += 1)}`;
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

  /**
   * El control vive en el light DOM. Necesita heredar el id y los atributos ARIA
   * que el marco calcula, asi que se ajustan aqui en vez de(slotearlo.
   */
  private control(): Element | null {
    for (const child of Array.from(this.children)) {
      if (['INPUT', 'SELECT', 'TEXTAREA'].includes(child.tagName)) return child;
    }
    return null;
  }

  protected render(): string {
    const id = this.fieldId;
    const hasError = this.error !== '';
    const help = this.help;
    const error = this.error;
    const required = this.required;

    const control = this.control();
    if (control !== null) {
      control.setAttribute('id', `${id}-control`);
      if (required) control.setAttribute('required', '');
      if (hasError) control.setAttribute('aria-invalid', 'true');
      else control.removeAttribute('aria-invalid');

      const describedBy = hasError ? `${id}-error` : help !== '' ? `${id}-help` : '';
      if (describedBy !== '') control.setAttribute('aria-describedby', describedBy);
      else control.removeAttribute('aria-describedby');
    }

    this.setAttribute('data-invalid', String(hasError));

    return `
      <div class="wrapper" part="wrapper">
        <label class="label" part="label" for="${this.escape(`${id}-control`)}">
          ${this.escape(this.label)}${required ? '<span class="required" part="required" aria-hidden="true">*</span>' : ''}
        </label>
        <slot></slot>
        ${
          hasError
            ? `<p class="error" part="error" id="${this.escape(`${id}-error`)}" role="alert">${this.escape(error)}</p>`
            : help !== ''
              ? `<p class="help" part="help" id="${this.escape(`${id}-help`)}">${this.escape(help)}</p>`
              : ''
        }
      </div>
    `;
  }

  protected styles(): string {
    // `:host` usa `--field-border` para que el light DOM (que no atraviesa el
    // shadow) herede el mismo borde que el marco.
    return `
      :host {
        display: block;
        font-family: var(--font-family-base);
        --field-border: var(--color-border);
        --field-border-focus: var(--color-info);
        --field-border-error: var(--color-danger);
      }

      :host([data-invalid='true']) { --field-border: var(--color-danger); }

      .wrapper {
        display: flex;
        flex-direction: column;
        gap: var(--space-1);
      }

      .label {
        color: var(--color-text);
        font-size: var(--font-size-sm);
        font-weight: var(--font-weight-medium);
        line-height: var(--line-height-normal);
      }

      .required { color: var(--color-danger); margin-inline-start: var(--space-1); }

      .help {
        margin: 0;
        color: var(--color-text-muted);
        font-size: var(--font-size-xs);
        line-height: var(--line-height-normal);
      }

      .error {
        margin: 0;
        color: var(--color-danger);
        font-size: var(--font-size-xs);
        line-height: var(--line-height-normal);
      }

      /* Estilo del control que el consumidor pone en el light DOM. El shadow no
         alcanza al light DOM, asi que se aplica desde el host. */
      :host ::slotted(input),
      :host ::slotted(select),
      :host ::slotted(textarea) {
        min-block-size: var(--target-touch-min);
        padding: var(--space-2) var(--space-3);
        border: var(--space-1) solid var(--field-border);
        border-radius: var(--radius-md);
        background: var(--color-background);
        color: var(--color-text);
        font: inherit;
        font-size: var(--font-size-sm);
        line-height: var(--line-height-normal);
      }

      :host ::slotted(input:focus-visible),
      :host ::slotted(select:focus-visible),
      :host ::slotted(textarea:focus-visible) {
        border-color: var(--field-border-focus);
        outline: var(--space-1) solid var(--field-border-focus);
        outline-offset: var(--space-1);
      }

      :host([data-invalid='true']) ::slotted(input),
      :host([data-invalid='true']) ::slotted(select),
      :host([data-invalid='true']) ::slotted(textarea) {
        border-color: var(--field-border-error);
      }
    `;
  }
}

define(TAG, SenaField);

export const register = (): void => {
  define(TAG, SenaField);
};

declare global {
  interface HTMLElementTagNameMap {
    'sena-field': SenaField;
  }
}