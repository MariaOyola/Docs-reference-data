# ADR-014: Evolucionar macroregion.name mediante expand/contract

**Estado:** PROPOSED  
**Fecha:** 2026-10-20  
**Última revisión:** 2026-10-21  
**Autora:** María José Rodríguez Oyola  
**Equipos involucrados:** Reference Data, Arquitectura y DevOps  
**Repositorio:** `design-software-reference-data-db`  
**Reto:** 13 — R6: evolución de esquema sin caída

> Número propuesto según el ZIP revisado; Arquitectura debe confirmar que ADR-014 siga disponible.

## Contexto

R6 exige modificar una tabla real con Liquibase, trasladar sus datos, mantener la compatibilidad del servicio y probar rollback en cuatro ambientes Docker.

Se eligió `institutional_structure.macroregion.name`, originalmente `VARCHAR(100) NOT NULL`. El cambio a `TEXT NOT NULL` demuestra una evolución por etapas; no amplía las validaciones de la API ni responde a un nuevo requisito de longitud. El backend debe seguir encontrando la columna `name`.

## Decisión

Se decide utilizar una columna temporal y sincronización en PostgreSQL, sin modificar el backend para R6.

| Changeset | Acción |
| --- | --- |
| `970_r6_expand_macroregion_name` | Agregar `name_v2 TEXT` nullable, conservando `name`. |
| `980_r6_sync_macroregion_name` | Instalar un trigger BEFORE INSERT OR UPDATE que copie `name` a `name_v2`. |
| `990_r6_backfill_macroregion_name` | Copiar los nombres existentes donde ambas columnas difieran. |
| `1000_r6_contract_macroregion_name` | Verificar sincronización, retirar trigger y función, establecer NOT NULL y reemplazar la columna anterior, conservando el nombre final `name`. |

```text
Original:    name VARCHAR(100) NOT NULL
Transición:  name VARCHAR(100) NOT NULL + name_v2 TEXT
Final:       name TEXT NOT NULL
```

El contract se registra después del backfill y se ejecuta en una transacción. En staging y main se verificó la separación mediante:

```text
Expansión, trigger y backfill:
--label-filter="R6 AND !contract" --context-filter="01_ddl,02_dml"

Contract:
--label-filter="R6 AND contract" --context-filter="04_tcl,r6-contract"
```

## Reversión

- **Contract:** reconstruye ambas columnas y el trigger con los nombres actuales. Cancela si algún nombre supera 100 caracteres, sin truncarlo.
- **Backfill:** conserva la copia mediante `SELECT 1`; no restaura los valores previos de la columna temporal.
- **Trigger:** elimina el trigger y después la función.
- **Expansión:** elimina únicamente `name_v2`, conservando los registros y `name`.

Las etiquetas marcan posiciones del historial; no son respaldos. La vista previa completa debe revisarse antes de revertir para evitar afectar cambios del equipo.

## Consecuencias

### Positivas

- Conserva el contrato SQL del backend durante la evolución.
- Mantiene sincronizadas las escrituras y permite comprobar cada fase.
- Ofrece migraciones y rollbacks trazables en Liquibase.

### Negativas y riesgos

- Duplica temporalmente almacenamiento y añade trabajo en cada escritura.
- El contract utiliza ACCESS EXCLUSIVE y bloquea temporalmente lecturas y escrituras. `lock_timeout = '3s'` limita la espera del bloqueo, no la duración total.
- La compatibilidad antes y después no demuestra ausencia de caída durante el cambio; falta medir solicitudes concurrentes.

## Alternativas consideradas

| Alternativa | Evaluación |
| --- | --- |
| Cambiar directamente el tipo | Más simple, pero no demuestra coexistencia y backfill. |
| Renombrar directamente a name_v2 | Rompe las consultas actuales del backend. |
| Copiar sin trigger | Las escrituras posteriores pueden dejar diferencias. |
| Adaptar el backend para ambas columnas | Excede el alcance autorizado de R6, limitado a la base de datos. |

## Validación

Las pruebas utilizaron PostgreSQL en Docker y los servicios e interfaces locales en Windows. Esta modalidad no redefine la topología oficial con Traefik ni equivale a promover ramas Git.

| Ambiente | Resultado observado |
| --- | --- |
| develop — 15432 | Contract aplicado, PAS creada desde pantalla y cuatro registros conservados al volver al esquema ampliado. |
| qa — 15433 | Cuatro changesets revertidos; volvió name VARCHAR(100) y ANT permaneció presente. |
| staging — 15434 | Backfill efectivo y contract aplicado; rollback conservó ANT y outbox. |
| main local — 15435 | Backfill efectivo y contract aplicado; rollback conservó ANT y outbox. |

**Incidencia en develop:** una etiqueta antigua revirtió también cuatro cambios ajenos a R6. Se reconstruyeron sus objetos y permisos; no se pudo determinar si outbox contenía eventos anteriores. Falta cerrar la evidencia de una repetición aislada: una salida de cero cambios no acredita una reversión efectiva.

En qa, seleccionar solo R6 aplicó también el contract. Las pruebas posteriores separaron las fases con los filtros anteriores.

El estado permanece PROPOSED hasta cerrar las verificaciones de develop, las pruebas de cancelación y la medición de disponibilidad. Las capturas y salidas detalladas se reúnen por separado.

## Referencias

- [Implementación en hu-r6-dev](https://github.com/code-sena/design-software-reference-data-db/tree/hu-r6-dev): changesets 970, 980, 990 y 1000 y sus rollbacks.
- [Resumen de implementación](../R6%20-%20Implementation%20overview%20for%20architects.md).
- [Plantilla central de ADR](https://github.com/code-sena/design-software-docs/blob/main/05-architecture/decisions/_template-adr.md) y [registro de decisiones](https://github.com/code-sena/design-software-docs/blob/main/05-architecture/decisions/README.md).
- [Decisiones de Reference Data](https://github.com/code-sena/design-software-docs/blob/main/09-microservices/services/02-reference-data-service/decisions.md) y [ADR-007: puertos y topología](https://github.com/code-sena/design-software-docs/blob/main/05-architecture/decisions/records/ADR-007-edge-gateway-and-port-convention.md).

La documentación central se revisó desde el ZIP proporcionado. La aprobación del ADR no autoriza automáticamente ejecutar el contract sobre bases compartidas o productivas.
