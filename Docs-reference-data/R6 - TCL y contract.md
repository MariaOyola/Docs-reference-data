# R6: TCL, transacciones y contract

## TCL y transacciones

**TCL (Transaction Control Language)** agrupa instrucciones para controlar transacciones. Una transacción permite confirmar varias operaciones juntas o deshacerlas juntas.

- `BEGIN`: inicia una transacción.
- `COMMIT`: confirma sus cambios.
- `ROLLBACK`: deshace sus cambios todavía no confirmados.

En esta migración, Liquibase administrará la transacción del changeset. Se configurará con `runInTransaction: true`; no añadiremos un `COMMIT` manual dentro del archivo SQL.

## Por qué el archivo está en 04_tcl

`ALTER TABLE`, `CREATE` y `DROP` son instrucciones **DDL**, aunque se ejecuten dentro de una transacción. La carpeta no cambia su clasificación.

El archivo `04_tcl/00_transaction_blocks/1000_r6_contract_macroregion_name.sql` se coloca en esa fase porque el changelog maestro incluye `04_tcl` después de `02_dml`, donde está el backfill. El orden lo determinan los includes de Liquibase, no el número `1000`.

La ubicación por sí sola no garantiza seguridad: el contract deberá habilitarse explícitamente y comprobar sus condiciones antes de retirar estructuras.

## Qué significa contract

**Contract es la fase que retira la estructura anterior cuando la nueva ya está preparada y verificada.** No es la fase que copia los datos antiguos; esa copia corresponde al backfill.

| Fase | Acción en macroregion |
| --- | --- |
| Expand | Agregar `name_v2 TEXT` conservando `name`. |
| Trigger | Copiar `name` a `name_v2` en cada INSERT o UPDATE. |
| Backfill | Completar los registros existentes que aún difieren. |
| Contract | Retirar el trigger y su función, conservar NOT NULL, eliminar `name` anterior y renombrar `name_v2` como `name`. |

```text
Antes del contract:   name VARCHAR(100) + name_v2 TEXT
Después del contract: name TEXT NOT NULL
```

Conservar el nombre final `name` permite mantener las consultas actuales del backend. El cambio de almacenamiento no amplía por sí solo las validaciones de la API.

## Condiciones para ejecutarlo

Antes del contract deben comprobarse datos sincronizados, dependencias, compatibilidad del servicio y un rollback preparado. La comprobación de datos se repite dentro de la transacción, después de adquirir el bloqueo, para evitar cambios entre la validación y el reemplazo.

El diseño usa `ACCESS EXCLUSIVE`, que bloquea temporalmente lecturas y escrituras. `lock_timeout = '3s'` limita la espera para adquirir bloqueos; **no limita la duración total de la migración**. El efecto sobre el servicio debe medirse, no darse por demostrado.

Si el changeset transaccional falla, PostgreSQL deshace sus cambios. Revertir un changeset que ya terminó requiere ejecutar su rollback de Liquibase, que deberá reconstruir el estado ampliado y rechazar nombres que no quepan en VARCHAR(100), sin truncarlos.

**Estado de esta nota:** explicación del diseño; no acredita que el contract o su rollback ya estén implementados y probados.
