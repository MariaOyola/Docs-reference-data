/**
 * `<sena-remote-unavailable>` — E1 de HU-SHELL-006.
 *
 * Pantalla de error de un remote. Reemplaza al `RemoteUnavailable` que el shell
 * llevaba duplicado en `src/shell/components/RemoteUnavailable/`.
 *
 * Uso (ADR-009 §8):
 *
 *   <ErrorBoundary fallback={<sena-remote-unavailable domain="iam" />} />
 *
 * Contrato de atributos:
 *   - `domain`  Requerido. Nombre del remote.
 *   - `reason`  Opcional. Detalle del error. Se escapa, no se interpola en HTML.
 *   - `retry-label` Opcional. Texto del boton. Por defecto "Reintentar".
 *
 * Interaccion: el boton emite `sena-retry` (`CustomEvent`, `composed: true` para
 * que salga del shadow DOM). Quien lo escucha decide como reintentar, porque solo
 * el host conoce la funcion de carga del remote:
 *
 *   <sena-remote-unavailable
 *     domain="iam"
 *     onSenaRetry={() => retryIam()}
 *   />
 *
 * Accesible: `role="alert"` para que el fallo se anuncie de inmediato, y el boton
 * tiene un area tactil de 44px (`--target-touch-min`).
 */
import { SenaElement, define } from './base-element';

const TAG = 'sena-remote-unavailable';

export interface RemoteUnavailableDetail {
  domain: string;
  reason?: string;
}

export class RemoteUnavailable extends SenaElement {
  static get observedAttributes(): string[] {
    return ['domain', 'reason', 'retry-label'];
  }

  get domain(): string {
    return this.attr('domain', 'remote');
  }

  set domain(value: string) {
    this.setAttribute('domain', value);
  }

  get reason(): string {
    return this.attr('reason');
  }

  set reason(value: string) {
    this.setAttribute('reason', value);
  }

  connectedCallback(): void {
    this.update();
  }

  attributeChangedCallback(): void {
    if (this.isConnected) this.update();
  }

  protected attributeChangedCallbackFor(): void {
    /* no-op: no hay estado interno */
  }

  /** Reintento. El listener decide que significa reintentar. */
  protected onRetry(): void {
    this.emit<RemoteUnavailableDetail>('sena-retry', {
      domain: this.domain,
      ...(this.reason !== '' ? { reason: this.reason } : {}),
    });
  }

  protected render(): string {
    const domain = this.escape(this.domain);
    const reason = this.reason;
    const retryLabel = this.attr('retry-label', 'Reintentar');

    const reasonBlock =
      reason !== ''
        ? `<p class="reason" part="reason">${this.escape(reason)}</p>`
        : '';

    return `
      <div class="wrapper" part="wrapper" role="alert">
        <span class="icon" part="icon" aria-hidden="true">!</span>
        <h2 class="title" part="title">No se pudo cargar ${domain}</h2>
        ${reasonBlock}
        <button class="retry" part="retry" type="button">
          ${this.escape(retryLabel)}
        </button>
      </div>
    `;
  }

  protected styles(): string {
    // Ningun color ni espaciado literal: todo sale de tokens (E4).
    return `
      :host {
        display: flex;
        align-items: center;
        justify-content: center;
        padding: var(--space-8);
        font-family: var(--font-family-base);
      }

      .wrapper {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--space-3);
        max-inline-size: var(--layout-content-max);
        text-align: center;
        color: var(--color-text);
      }

      .icon {
        display: flex;
        align-items: center;
        justify-content: center;
        inline-size: var(--target-touch-min);
        block-size: var(--target-touch-min);
        border-radius: var(--radius-full);
        background: var(--color-danger-surface);
        color: var(--color-danger);
        font-size: var(--font-size-xl);
        font-weight: var(--font-weight-bold);
      }

      .title {
        margin: 0;
        font-size: var(--font-size-h3);
        font-weight: var(--font-weight-bold);
      }

      .reason {
        margin: 0;
        color: var(--color-text-muted);
        font-size: var(--font-size-xs);
      }

      .retry {
        min-block-size: var(--target-touch-min);
        margin-block-start: var(--space-2);
        padding: var(--space-2) var(--space-5);
        border: var(--space-1) solid var(--color-brand);
        border-radius: var(--radius-md);
        background: var(--color-brand);
        color: var(--color-background);
        font: inherit;
        font-weight: var(--font-weight-medium);
        cursor: pointer;
      }

      .retry:hover { background: var(--color-brand-hover); border-color: var(--color-brand-hover); }
      .retry:active { background: var(--color-brand-strong); border-color: var(--color-brand-strong); }
      .retry:focus-visible { outline: var(--space-1) solid var(--color-info); outline-offset: var(--space-1); }
    `;
  }

  /**
   * Delegacion de eventos en lugar de `addEventListener` por render: el listener
   * se registra una vez en el shadow root y sobrevive a cada repintado.
   *
   * Va en `this.root` y no en `this` a proposito: un click que viene de dentro
   * del shadow se re-targetea al host al cruzar la frontera, asi que escuchando
   * en el host `event.target` seria el propio `<sena-remote-unavailable>` y
   * `closest('.retry')` no encontraria nada.
   */
  protected override initialize(): void {
    this.root.addEventListener('click', (event) => {
      const target = event.target as HTMLElement | null;
      if (target === null) return;
      if (target.closest('.retry') === null) return;
      this.onRetry();
    });
  }
}

define(TAG, RemoteUnavailable);

/** Registra el elemento. Idempotente. */
export const register = (): void => {
  define(TAG, RemoteUnavailable);
};

declare global {
  interface HTMLElementTagNameMap {
    'sena-remote-unavailable': RemoteUnavailable;
  }
}