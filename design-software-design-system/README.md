# design-software-design-system

Paquete compartido de tokens y componentes visuales. Su núcleo es framework-agnóstico
(TS puro, sin React), conforme a ADR-009 §1.

## Estado actual

| Capa | Estado | HU |
|---|---|---|
| **Tokens** | ✅ Implementado | `HU-SHELL-005` |
| **Componentes base** | ✅ Implementado | `HU-SHELL-006` |

> `NotificationBell`, `NotificationPanel` y `OfflineBanner` **no** son alcance de HU-SHELL-006:
> la HU solo pide `RemoteUnavailable`, `LoadingRemote` y los primitivos. Siguen duplicados en el
> shell y quedan para una HU posterior.

## Componentes (HU-SHELL-006)

Son **Web Components** (Custom Elements), no componentes React. ADR-009 §1 exige que este
paquete sea framework-agnóstico, y un remote puede ser React hoy y otro framework mañana.

| Etiqueta | Cubre | HU |
|---|---|---|
| `<sena-remote-unavailable domain="iam">` | Pantalla de error de un remote | E1 |
| `<sena-loading-remote domain="iam">` | Estado de carga de un remote | E2 |
| `<sena-status-badge tone="success" label="Publicado">` | Badge de estado | E3 |
| `<sena-button variant="primary">` | Botón base | E3 |
| `<sena-field label="Correo" error="...">` | Campo de formulario | E3 |

### Registrar

```ts
import '@sena/design-system/components';
```

Cada componente se autoregistra al importarse. El import es **explícito** y no ocurre en el
entrypoint principal, para que importar los tokens en un test de Node no toque
`customElements`.

### Usar desde React

Como JSX no conoce los intrinsic elements, hay que declararlos una vez:

```tsx
// src/vite-env.d.ts
import type { SenaButton, SenaField, LoadingRemote, RemoteUnavailable, StatusBadge } from '@sena/design-system/components';

declare global {
  namespace JSX {
    interface IntrinsicElements {
      'sena-button': React.DetailedHTMLProps<React.HTMLAttributes<SenaButton>, SenaButton> & { variant?: string };
      'sena-field': React.DetailedHTMLProps<React.HTMLAttributes<SenaField>, SenaField> & { label: string; error?: string };
      'sena-loading-remote': React.DetailedHTMLProps<React.HTMLAttributes<LoadingRemote>, LoadingRemote>;
      'sena-remote-unavailable': React.DetailedHTMLProps<React.HTMLAttributes<RemoteUnavailable>, RemoteUnavailable>;
      'sena-status-badge': React.DetailedHTMLProps<React.HTMLAttributes<StatusBadge>, StatusBadge> & { tone?: string; label: string };
    }
  }
}
```

### Contratos

| Componente | Atributos | Evento |
|---|---|---|
| `sena-remote-unavailable` | `domain` (req.), `reason`, `retry-label` | `sena-retry` al pulsar Reintentar |
| `sena-loading-remote` | `domain` (req.), `label` | — |
| `sena-status-badge` | `tone`, `label` (req.), `icon` | — |
| `sena-button` | `variant`, `disabled`, `type` | — (envuelve un `<button>` nativo) |
| `sena-field` | `label` (req.), `help`, `error`, `required`, `id` | — (el control va en light DOM) |

`tone` es el contrato público del badge: `success | info | warning | danger | neutral | unknown`.
Un valor desconocido cae en `unknown`, nunca rompe el render.

### E4: nada de valores literales

Ningún componente hardcodea un color ni un espaciado: todo sale de `var(--token)`. El CI lo
verifica:

```bash
npm run check:no-hardcode
```

El script falla ante un color literal, una medida literal, o un `var(--x)` que apunte a un
token que el design-system no publica.

### Accesibilidad

- `sena-loading-remote`: `role="status"` + `aria-live="polite"`.
- `sena-remote-unavailable`: `role="alert"`; el error se anuncia al aparecer.
- `sena-status-badge`: siempre **color + icono + texto**, nunca solo color (lo exige el spec).
  Un tono desconocido se dibuja con borde punteado para que no se lea como "un estado más".
- `sena-button` y `sena-field`: área táctil de `44px` (`--target-touch-min`) y `:focus-visible`.
- `prefers-reduced-motion` respetado en los dos componentes con animación.

## Tokens

60 tokens de color, tipografía, espaciado, radio, sombra, z-index y layout.

### Fuente canónica

```text
design-software-docs/12-ux-ui/mockup-ddd/design-tokens.md   ← spec ratificado (locked, WCAG AA)
design-software-pages/v2/assets/tokens.css                   ← implementación canónica de valores
```

Ambos son **solo lectura**: este repo no los modifica.

Los repos son privados y el `GITHUB_TOKEN` de GitHub Actions no puede leerlos, así que
`spec/` guarda una **copia versionada** de los dos. Esa copia es lo que usa el CI.

| | Repos de origen | Copia del CI |
|---|---|---|
| Spec | `design-software-docs/12-ux-ui/mockup-ddd/design-tokens.md` | `spec/design-tokens.md` |
| Tokens canónicos | `design-software-pages/v2/assets/tokens.css` | `spec/tokens.css` |

### Refrescar la copia

```bash
npm run sync:spec    # copia desde los repos hermanos y regenera
```

Requiere `design-software-docs` y `design-software-pages` clonados junto a este repo.
**Hay que correrlo cuando el spec cambie arriba**, y commitear el resultado: el CI no
detecta esa deriva por sí solo.

### Generar

```bash
npm run generate:tokens   # regenera src/tokens/tokens.generated.{ts,css}
npm run check:tokens      # falla si están desactualizados (CI)
```

**Los archivos `tokens.generated.*` no se editan a mano.** Se edita la fuente y se regenera.

### Consumir

```ts
import { color, space, radius, tokens } from '@sena/design-system';

color.brand;        // '#007832'
space[4];           // '16px'
radius.md;          // '10px'
tokens.color_brand; // '#007832'
```

En CSS:

```css
@import '@sena/design-system/tokens.css';

.miClase {
  color: var(--color-brand);
  padding: var(--space-4);
}
```

### Tipado estricto (E3)

Los tokens se exportan como unión de literales, no como `Record<string, string>`. Un nombre
mal escrito **falla en `tsc`**, no en runtime:

```ts
tokens.color_brnad   // ❌ error de compilación
tokens.color_brand    // ✅
```

### Estados de negocio

`businessState` expone los cuatro estados del catálogo con su color y superficie:

| Estado | Color | Superficie |
|---|---|---|
| `BORRADOR` | `color-text-muted` | `color-surface-muted` |
| `EN_REVISION` | `color-warning` | `color-warning-surface` |
| `PUBLICADO` | `color-success` | `color-success-surface` |
| `CONFLICTO` | `color-conflict` | `color-danger-surface` |

La UI **no debe hardcodear la lista**: viene del catálogo parametrizable `status` de
backend. Aquí está solo la representación visual. El spec exige siempre
**color + icono + texto**, nunca solo color.

## Scripts

```bash
npm run generate:tokens   # regenera los tokens desde la fuente
npm run check:tokens      # verifica que no estén desactualizados
npm run check:no-hardcode # E4: ningún componente con color/espaciado literal
npm run sync:spec         # refresca spec/ desde los repos de origen
npm run typecheck
npm test                  # incluye el test de divergencia contra el spec
npm run lint
npm run verify            # todo lo anterior, en orden
```

## Regla

Ningún MFE redefine tokens ni estilos globales. Los estilos de cada MFE usan CSS Modules.

`web-shell/src/styles/tokens.test.ts` verifica esta regla: falla si el shell redefine un
token del spec con valor literal, si un token propio no lleva prefijo `shell-`, o si aparece
una auto-referencia.

## Deuda

- **Falta acceso al spec privado desde el CI**, para activar el job de deriva. El job ya
  está escrito y comentado en `.github/workflows/spec-drift.yml`: clona
  `design-software-docs`, refresca `spec/` y falla si quedó desactualizado. Sin acceso
  no se puede activar, porque el `GITHUB_TOKEN` de Actions no puede leer repos privados.

  Verificado 2026-10-13: **`design-software-pages` es público** (se lee sin
  credenciales) y **solo `design-software-docs` es privado**.

  Un PAT fine-grained **no** se puede scoped a `code-sena` desde una cuenta que no es
  miembro de la organización: GitHub solo ofrece en *Resource owner* las orgs con
  fine-grained PATs habilitado. Alternativas a decidir con arquitectura: deploy key SSH
  de solo lectura (recomendado), PAT clásico de scope `repo`, o publicar el spec ratificado.

  Paso a paso completo: **`DEUDA-spec-repo-access.md`** (raíz de `Propuesta-arquitectura-front`).

- **20 repos de backend y db sin CI.** Tienen `CODEOWNERS` pero ningún pipeline
  ejecuta sus tests. Es infraestructura, no este repo.
- **5 de 10 micros frontend siguen con tokens propios**, con valores divergentes
  (`actors-mfe` incluso tiene su propia paleta con nombres inventados). Migrarlos cambia su
  UI y es trabajo de cada dueño.
- **Los 5 `StatusBadge` duplicados en los MFEs** (`academic-management-mfe` ×2,
  `document-mfe` ×2, `monitoring-mfe` ×1) ya tienen replacement en
  `<sena-status-badge>`. Ninguno migrado todavía: cada dueño reemplaza su componente y borra
  su CSS. Los tonos ya coinciden, así que el cambio es mecánico.
- **Las clases `.primaryButton` / `.field` / `.badge` duplicadas** en 8+ lugares tienen
  replacement en `<sena-button>`, `<sena-field>` y `<sena-status-badge>`. Al ser CSS y no
  componentes React, la migración es por vista, no por MFE entero.
- **Ningún MFE declara `@sena/design-system` como dependencia.** Para consumirlo hace falta
  añadirlo al `package.json` y al `tsconfig.json` de cada uno. El shell ya lo hace.
- `web-shell` conserva su propia escala de espaciado con prefijo `shell-`: el spec usa los
  mismos nombres con otros valores y adoptarla movía el layout.
- `mockup-ddd/design-tokens.md` marca pendientes la ratificación oficial de marca SENA. Si
  cambia el verde o la tipografía, hay que correr `npm run sync:spec` y regenerar.