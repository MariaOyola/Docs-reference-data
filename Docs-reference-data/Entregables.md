# Entregables R6

## 1. Resumen

| Entrega | Qué debe contener |
|---|---|
| Pull request propio | Tu implementación de R6 sobre `reference-data-db` |
| ADR (registro de decisión) | Qué cambio elegiste, por qué y cómo resuelves compatibilidad, transición y reversión |
| Informe técnico breve en el repositorio | Evidencias: capturas, métricas y salidas de pruebas que demuestren los cuatro criterios |
| Sustentación individual de diez minutos | Explicar tu solución y realizar en vivo una modificación solicitada por el instructor |

---

## 2. Checklist por entrega

### Pull request propio
- [ ] Implementación de R6 sobre `reference-data-db`
- [ ] Changesets de Liquibase (expansión, backfill, contracción)
- [ ] Rollback definido para cada changeset
- [ ] Probado en los cuatro ambientes: develop, qa, staging y main
- [ ] Descripción del PR que enlaza el ADR y el informe

### ADR (registro de decisión)
- [ ] Qué cambio elegiste
- [ ] Por qué lo elegiste (alternativas descartadas)
- [ ] Cómo resuelves la **compatibilidad**
- [ ] Cómo resuelves la **transición**
- [ ] Cómo resuelves la **reversión**

### Informe técnico breve (en el repositorio)
- [ ] Capturas de pantalla
- [ ] Métricas
- [ ] Salidas de las pruebas
- [ ] Evidencia de los **cuatro criterios** de R6

### Sustentación individual (10 minutos)
- [ ] Explicar la solución sin apuntes
- [ ] Hacer en vivo una modificación pedida por el instructor
- [ ] Poder justificar cada decisión del PR y del ADR

---

## 3. Cómo se conectan

```
PR  ──implementa──▶  ADR  ──se demuestra con──▶  Informe  ──se defiende en──▶  Sustentación
```

El ADR explica el porqué, el PR es el cómo, el informe es la prueba y la sustentación comprueba que lo entiendes.

---

## 4. Seguimiento

| Entrega | Tiempo del plan | Estado |
|---|---|---|
| Pull request | Incluido en Implementación de R6 (4 h) | ☐ |
| ADR e informe | 1 h 30 min (junto con el PR) | ☐ |
| Sustentación | 1 h 30 min | ☐ |