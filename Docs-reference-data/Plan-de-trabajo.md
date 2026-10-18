# Plan de estudio R6: 16 horas efectivas

## 1. Distribución del tiempo

| Bloque | Tiempo | Qué debes lograr |
|---|---|---|
| Funcionamiento de extremo a extremo | 2 h | Levantar el dominio, ejecutar un flujo y reconocer dónde verificar los datos |
| Fundamentos de R6 | 2 h | Entender expansión, backfill, compatibilidad, contracción y rollback |
| Tu tabla y Liquibase | 2 h | Conocer columnas, restricciones, changesets y orden de ejecución |
| Implementación de R6 | 4 h | Construir la migración y entender cada decisión |
| Pruebas y cuatro ambientes | 3 h | Demostrar continuidad, coherencia de datos y reversión |
| ADR, informe y PR | 1 h 30 min | Dejar documentada la entrega |
| Sustentación y ejercicios sorpresa | 1 h 30 min | Explicar sin apuntes y modificar tu solución |
| **Total** | **16 h** | |

> **Nota:** las 2 horas de integración son una estimación si los componentes funcionan. Si aparece un bloqueo, hay que resolverlo para poder demostrar R6.

---

## 2. Método de estudio (para cada tema)

1. **Ejemplo sencillo:** te lo explico con un caso pequeño.
2. **Tus palabras:** tú lo explicas sin leer.
3. **Aplicación:** lo llevamos a tu base de datos real.
4. **Práctica:** lo haces tú, sin copiar.
5. **Prueba:** te planteo un fallo o un cambio para comprobar que lo entendiste.

---

## 3. La idea central: cambiar sin romper

R6 se basa en un principio: **nunca cambies la base de datos y el código en el mismo paso.** Mientras haces el cambio, hay versiones del servicio viejas y nuevas corriendo a la vez, y ambas deben seguir funcionando.

Por eso el cambio se hace en fases (patrón *expand / contract*):

```
EXPANDIR  →  BACKFILL  →  COMPATIBILIDAD  →  CONTRAER
 (agregar)    (copiar)     (conviven ambas)    (retirar lo viejo)
                                  ↑
                          ROLLBACK posible
                       hasta antes de contraer
```

### Ejemplo sencillo: renombrar una columna

Tienes una tabla `cliente` con la columna `telefono` y quieres que se llame `celular`.

**El camino peligroso (todo de golpe):**

```sql
ALTER TABLE cliente RENAME COLUMN telefono TO celular;
```

Al ejecutarlo, la versión vieja del servicio sigue consultando `telefono`, que ya no existe, y falla. Hay caída.

**El camino seguro, por fases:**

| Fase | Qué se hace | Estado del sistema |
|---|---|---|
| 1. Expansión | Se agrega `celular` (nullable) sin tocar `telefono` | El código viejo no nota nada |
| 2. Backfill | Se copian los datos de `telefono` a `celular` | Ambas columnas tienen datos |
| 3. Compatibilidad | El código nuevo escribe en ambas y lee de `celular` | Versión vieja y nueva conviven |
| 4. Contracción | Cuando nadie usa `telefono`, se elimina | Queda solo `celular` |
| Rollback | Si algo falla antes de contraer, se vuelve atrás sin perder datos | Por eso se contrae al final |

### Qué significa cada palabra

- **Expansión:** agregar lo nuevo sin quitar lo viejo. Es un cambio aditivo y seguro.
- **Backfill:** llenar lo nuevo con los datos históricos que ya existían.
- **Compatibilidad:** periodo en que lo viejo y lo nuevo funcionan a la vez.
- **Contracción:** retirar lo viejo. Es el paso destructivo, por eso va al final.
- **Rollback:** poder deshacer el cambio. Solo es limpio mientras no hayas contraído.

---

## 4. Tu turno (paso 2 del método)

Responde con tus palabras, sin copiar lo de arriba:

1. ¿Por qué no es buena idea hacer el `RENAME` directo?
2. ¿Por qué la contracción va al final y no al principio?
3. ¿Qué pasa si falla el backfill a la mitad? ¿Qué debería garantizar para poder repetirlo?
4. ¿Hasta qué fase puedes hacer rollback sin perder datos?

Cuando respondas, corrijo lo que haga falta y pasamos al paso 3: aplicarlo a **tu tabla**.

---

## 5. Checklist de avance

- [ ] Funcionamiento de extremo a extremo (2 h)
- [ ] Fundamentos de R6 (2 h)
- [ ] Tu tabla y Liquibase (2 h)
- [ ] Implementación de R6 (4 h)
- [ ] Pruebas y cuatro ambientes (3 h)
- [ ] ADR, informe y PR (1 h 30 min)
- [ ] Sustentación y ejercicios sorpresa (1 h 30 min)