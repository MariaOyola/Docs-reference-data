<!-- RESUMEN-EJECUTIVO
agente: Jesús Ariel González Bonilla (PM + Arquitecto)
capacidad: design tokens ratificados (placeholder locked) para el mockup
fase: diseño (UX/UI)
estado: accepted (placeholder locked)
dependencias_entrada: 12-ux-ui/design-system.md; mockups aceptados v1/v2 (valores WCAG verificados)
consumidores_siguientes: generación del mockup (consistencia visual), design-system del frontend real
tldr: Valores concretos de color/tipografía/espaciado FIJADOS para el mockup, tomados de los mockups ya aceptados y verificados WCAG AA. Pendientes de ratificación oficial de marca SENA, pero locked para consistencia.
decisiones_clave: verde #007832 como marca; estados con color+icono+texto; contrastes AA verificados; tokens compartidos por todos los MFE
halts_registrados: color/tipografía oficiales SENA pendientes de lineamiento
-->

# Design tokens (ratificados — placeholder locked)

> **ESTADO: locked para el mockup.** Valores concretos tomados de los mockups **ya aceptados**
> (dashboard/horarios v2) y **verificados WCAG 2.1 AA**. Quedan **fijos** para que el mockup sea
> visualmente consistente. **Pendiente:** ratificación del lineamiento oficial de marca SENA
> (si cambia el verde/tipografía, se actualiza aquí y se regenera). Complementa
> [design-system.md](../design-system.md).

> **Implementación canónica:** `mockups/app/assets/tokens.css` es la fuente de los valores
> exactos. Además del subconjunto ratificado que lista este doc, `tokens.css` define
> `--color-brand-hover` / `--color-brand-soft`, la escala tipográfica completa
> (`--font-size-xs…3xl`, pesos, line-heights), la escala de espaciado (`--space-1…8`),
> sombras (`--shadow-sm/md`), z-index y dimensiones de layout
> (`--topbar-height: 72px`, `--sidebar-width: 264px`, `--sidebar-collapsed-width: 76px`).
> Este documento lista el subconjunto ratificado; `tokens.css` manda para los valores exactos.

## Color

| Token | Valor | Uso | Contraste (sobre blanco) |
|---|---|---|---|
| `color-brand` | `#007832` | Verde institucional (marca, acción primaria, éxito) | 5.63:1 ✅ |
| `color-background` | `#FFFFFF` | Fondo de página | — |
| `color-surface` | `#FFFFFF` | Tarjetas | — |
| `color-surface-muted` | `#F2F4F7` | Fondos sutiles / filas alternas | — |
| `color-text` | `#101828` | Texto principal | 17.75:1 ✅ |
| `color-text-muted` | `#475467` | Texto secundario | 7.69:1 ✅ |
| `color-border` | `#EAECF0` | Bordes/divisores | — |
| `color-danger` / `color-conflict` | `#B42318` | Crítico / **conflicto** | 6.57:1 ✅ |
| `color-danger-surface` | `#FEF3F2` | Fondo de banda/badge crítico | — |
| `color-warning` | `#93370D` | Advertencia / EN_REVISIÓN | 7.21:1 (sobre `#FFFAEB`) ✅ |
| `color-warning-surface` | `#FFFAEB` | Fondo de aviso | — |
| `color-success` | `#067647` | Éxito / PUBLICADO | ✅ |
| `color-success-surface` | `#ECFDF3` | Fondo de éxito | — |
| `color-info` | `#175CD3` | Informativo / enlaces | ✅ |
| `color-info-surface` | `#EFF8FF` | Fondo informativo | — |

### Estados de negocio (siempre color + icono + texto, nunca solo color)
| Estado | Color | Superficie | Icono sugerido |
|---|---|---|---|
| BORRADOR | `color-text-muted` | `color-surface-muted` | círculo/lápiz |
| EN_REVISIÓN | `color-warning` | `color-warning-surface` | reloj/alerta |
| PUBLICADO | `color-success` | `color-success-surface` | check |
| **CONFLICTO** | `color-conflict` | `color-danger-surface` | triángulo de alerta |

> Los estados de negocio provienen del catálogo parametrizable `status`; la UI **no** hardcodea la
> lista (design-system §3 / modeling-conventions §1). Estos tokens son solo la representación visual.

## Tipografía

| Token | Valor |
|---|---|
| `font-family-base` | `system-ui, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif` (rendimiento en conectividad baja) |
| `font-size-xs … 2xl` | `12 · 14 · 16 · 18 · 20 · 24 · 30 px` |
| `font-weight` | `regular 400 · medium 500 · bold 700` |
| `line-height` | `tight 1.25 · normal 1.5 · relaxed 1.7` |

## Espaciado, forma, elevación

| Grupo | Valores |
|---|---|
| `space-1…8` | escala base 4px: `4 · 8 · 12 · 16 · 24 · 32 · 40 · 48 px` |
| `radius` | `sm 6px · md 10px · lg 16px` |
| `shadow` | `sm 0 1px 2px rgba(16,24,40,.06)` · `md 0 4px 8px rgba(16,24,40,.08)` |
| `z-index` | `base 0 · dropdown 1000 · overlay 1010 · modal 1020 · toast 1030` |

## Tokens añadidos en HU-SHELL-006

Necesarios para `RemoteUnavailable` y `LoadingRemote` (ADR-009 §8). No estaban en el spec
original: el shell los tenía comoalias de su escala `shell-`, que el spec prohíbe
("un solo set, ningún MFE los redefine", §Regla).

| Token | Valor | Uso | Origen |
|---|---|---|---|
| `color-brand-strong` | `#006D2E` | Verde pulsado/`:active` de la acción primaria |-shell-primary-600 |
| `font-size-h3` | `23px` | Titular de tarjeta y encabezado de estado | `shell-font-size-h3` |
| `radius-full` | `9999px` | Píldoras, avatares y badges circulares | `shell-radius-full` |
| `target-touch-min` | `44px` | Objetivo táctil mínimo (WCAG 2.5.5, breakpoint `tablet` §Responsive) | `shell-target-touch-min` |
| `layout-content-max` | `32rem` | Ancho máximo del bloque de contenido legible en pantallas de estado | `shell-space-12` |

> **No se duplican los que ya existen.** El shell también usaba `--space-10` (40px),
> `--space-12` (48px) y `--font-size-caption` (12px), que son exactamente `--space-7`,
> `--space-8` y `--font-size-xs`. Esos tres quedan como alias del token canónico, no como
> tokens nuevos: añadir dos nombres para 40px y 48px contradice la §Regla de este documento.

## Responsive (breakpoints)

| Nombre | Ancho | Uso |
|---|---|---|
| `mobile` | < 768px | 1 columna, tabla→tarjetas, nav en drawer/hamburguesa; PNG de referencia ~390px |
| `tablet` | 768–1024px | 2 columnas; objetivos táctiles ≥44px (uso en ambientes) |
| `desktop` | > 1024px | layout completo; PNG de referencia ~1440px |

## Regla
Estos tokens son **compartidos** por todos los micro-frontends (un solo set). Ningún MFE los
redefine (ver [component-inventory.md](component-inventory.md) §C).
