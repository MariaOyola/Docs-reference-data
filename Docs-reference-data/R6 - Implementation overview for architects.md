# R6: evolución del esquema de Reference Data — resumen de implementación

Mi trabajo individual corresponde al **reto 13 — R6: evolución de esquema sin caída**, aplicado a `design-software-reference-data-db`.

Los requisitos asignados son:

1. Una migración expand/contract con Liquibase sobre una tabla real del dominio con datos.
2. Carga de datos de prueba y backfill.
3. Compatibilidad del servicio con las versiones anterior y nueva del esquema durante la migración.
4. Pruebas de rollback en los cuatro ambientes Docker: develop, qa, staging y main.

El flujo implementado funciona localmente: el microfrontend de Reference Data carga dentro del web-shell y las operaciones reales de la interfaz llegan a PostgreSQL mediante el backend existente. Los resultados siguientes describen la migración implementada y las comprobaciones realizadas para este reto.

## Alcance y propósito

La implementación cambia `institutional_structure.macroregion.name` de `VARCHAR(100) NOT NULL` a `TEXT NOT NULL`. Utiliza una columna temporal `name_v2` para demostrar una migración por etapas, conservando el nombre de columna que espera el backend existente. Este cambio no amplía los límites actuales de validación de entrada de la API.

El propósito es demostrar el procedimiento de evolución de esquema exigido por R6. No se atribuye al cambio una mejora de rendimiento ni un requisito funcional aprobado para admitir nombres mayores de 100 caracteres.

## Etapas de la migración

1. **Expansión (expand) — `970_r6_expand_macroregion_name`:** agrega `name_v2 TEXT`, permitiendo NULL y conservando la columna original y los registros.
2. **Sincronización — `980_r6_sync_macroregion_name`:** instala una función de PostgreSQL y un trigger BEFORE INSERT OR UPDATE que copia `name` a `name_v2`.
3. **Copia de datos existentes (backfill) — `990_r6_backfill_macroregion_name`:** copia los nombres únicamente donde las columnas difieren, incluidos los valores NULL.
4. **Retirada (contract) — `1000_r6_contract_macroregion_name`:** comprueba la sincronización, elimina el trigger y la función temporales, establece NOT NULL, elimina la columna original y renombra `name_v2` como `name`.

El contract se ejecuta dentro de una transacción y utiliza un tiempo máximo de espera de tres segundos para adquirir bloqueos. Este límite no restringe la duración total de la migración. El bloqueo ACCESS EXCLUSIVE impide temporalmente las lecturas y las escrituras.

Su rollback reconstruye ambas columnas y el mecanismo de sincronización. Rechaza nombres mayores de 100 caracteres en lugar de truncarlos. El rollback del backfill conserva intencionalmente los valores copiados; la reversión posterior de la expansión elimina la columna temporal. No restaura de forma independiente el contenido anterior de `name_v2`.

## Archivos de implementación agregados

```text
01_ddl/04_alter/970_r6_expand_macroregion_name.sql
01_ddl/09_triggers/980_r6_sync_macroregion_name.sql
02_dml/01_updates/990_r6_backfill_macroregion_name.sql
04_tcl/00_transaction_blocks/1000_r6_contract_macroregion_name.sql

05_rollbacks/01_ddl/04_alter/970_r6_expand_macroregion_name.rollback.sql
05_rollbacks/01_ddl/09_triggers/980_r6_sync_macroregion_name.rollback.sql
05_rollbacks/02_dml/01_updates/990_r6_backfill_macroregion_name.rollback.sql
05_rollbacks/04_tcl/00_transaction_blocks/1000_r6_contract_macroregion_name.rollback.sql
```

Todas las rutas anteriores y siguientes son relativas a `design-software-reference-data-db`.

## Changelogs modificados para registrar las migraciones

```text
01_ddl/04_alter/0000changelog.yaml
01_ddl/09_triggers/0000changelog.yaml
02_dml/01_updates/0000changelog.yaml
04_tcl/00_transaction_blocks/0000changelog.yaml
```

El contract está registrado después de la fase DML, por lo que el changelog maestro lo encuentra después del backfill. Sus instrucciones siguen siendo DDL aunque el archivo esté ubicado en `04_tcl`.

Una limpieza de idioma independiente tradujo al inglés los campos descriptivos `comment` de los siguientes changelogs adicionales, sin cambiar operaciones SQL, identificadores ni datos del dominio:

```text
02_dml/00_inserts/0000changelog.yaml
02_dml/04_patches/0000changelog.yaml
03_dcl/00_roles/0000changelog.yaml
03_dcl/01_grants/0000changelog.yaml
```

## Ejecución local y justificación

PostgreSQL se ejecutó en Docker, mientras IAM, Reference Data, el microfrontend y el web-shell se ejecutaron localmente en Windows. Esta configuración permitió realizar operaciones reales desde la interfaz hasta PostgreSQL mediante el backend existente, sin modificar código del backend, frontend ni IAM para R6.

La aplicación existente continuó utilizando la columna `name` durante la expansión. El trigger sincronizó la columna temporal y el contract conservó el nombre final de la columna. Esto permitió comprobar la compatibilidad utilizando la aplicación existente, en lugar de un cliente sustituto o un flujo de negocio simulado.

Los cuatro ambientes Docker configurados utilizaron bases de datos y puertos separados:

| Ambiente | Proyecto Docker | Base de datos | Puerto del host |
| --- | --- | --- | ---: |
| develop | ds-develop | design-software-develop | 15432 |
| qa | ds-qa | design-software-qa | 15433 |
| staging | ds-staging | design-software-staging | 15434 |
| main local | ds-main | design-software-main | 15435 |

La migración se probó en los cuatro ambientes locales de base de datos Docker configurados, utilizando la rama de implementación. La ejecución del backend y la interfaz en Windows conservó la integración local existente, mientras PostgreSQL permaneció en contenedores. La promoción de ramas Git es una actividad de publicación independiente.

## Resultados de validación

| Área | Resultado observado |
| --- | --- |
| Validación de Liquibase | Los changelogs se validaron correctamente, también después de traducir los comentarios descriptivos. |
| Trigger | Se probó la sincronización en INSERT y UPDATE dentro de una transacción cuyos datos temporales se deshicieron mediante ROLLBACK. |
| Backfill | Staging y main contenían ANT antes de la expansión; la fase de expansión/backfill reportó una fila afectada y posteriormente ambas columnas de nombre coincidían. |
| Esquema final | El contract produjo `name TEXT NOT NULL`. |
| Integración con la aplicación | Se confirmaron consultas y creación de registros con el esquema final. PAS se creó desde la interfaz y se observó en PostgreSQL. |
| Rollback en qa | Se revirtieron los cuatro changesets R6; se restauró `name VARCHAR(100)` y ANT permaneció presente. |
| Rollback en staging | Se revirtieron los cuatro changesets R6; ANT conservó su UUID, código y nombre, y outbox permaneció presente. |
| Rollback en main local | Se revirtieron los cuatro changesets R6; ANT conservó su UUID, código y nombre, y outbox permaneció presente. |

Los resultados se basan en salidas de comandos y capturas registradas durante la ejecución local.

## Ejecución controlada y reversión

Liquibase registra los changesets ejecutados, mientras los filtros explícitos de etiquetas y contextos separan la fase de expansión del contract. Se revisaron las vistas previas completas del rollback para identificar los changesets seleccionados para revertir.

```text
Expansión, trigger y backfill:
--label-filter="R6 AND !contract" --context-filter="01_ddl,02_dml"

Contract:
--label-filter="R6 AND contract" --context-filter="04_tcl,r6-contract"
```

En develop, el rollback del contract restauró el esquema ampliado y conservó las cuatro macrorregiones, incluida PAS, creada mediante la interfaz después del contract. Una reversión inicial también afectó cuatro changesets ajenos a R6; estos se reaplicaron y se reconstruyeron la estructura outbox, el dato de demostración y los permisos. Reconstruir una tabla no recupera las filas históricas que pudiera haber contenido.

En qa, staging y main, el rollback ejecutado enumeró los cuatro changesets R6 en orden inverso: 1000, 990, 980 y 970. Las comprobaciones de staging y main también confirmaron que outbox permaneció presente.

## Commits publicados

| Commit | Propósito |
| --- | --- |
| cc89fb4 | Agregar la expansión y su rollback. |
| 8205610 | Agregar el trigger de sincronización. |
| 4346d21 | Agregar la migración de backfill. |
| 947d03f | Agregar el contract y su rollback en inglés. |
| 64052c3 | Traducir las descripciones de los changesets al inglés. |
| 64dd0bf | Incluir el SQL de rollback del trigger, que se había probado localmente pero faltaba en el archivo confirmado en Git. |

Rama de implementación: [hu-r6-dev](https://github.com/code-sena/design-software-reference-data-db/tree/hu-r6-dev).

## Alcance de la entrega

La implementación está publicada en `hu-r6-dev`. Este trabajo comprende los cambios de base de datos para R6 y su validación funcional local, utilizando la integración existente del servicio Reference Data, el microfrontend y el web-shell.
