# Informe del plan de mejora: integración de Reference Data y reto R6

**Autora:** María José Rodríguez Oyola  
**Fecha del informe:** 21 de octubre de 2026  
**Dominio:** Reference Data  
**Repositorio de implementación:** `design-software-reference-data-db`  
**Rama de trabajo:** `hu-r6-dev`

## 1. Objetivo y alcance

El trabajo comprende la preparación funcional del dominio y mi reto individual **13 — R6: evolución de esquema sin caída**. El reto requiere una migración expand/contract con Liquibase, datos de prueba y backfill, compatibilidad del servicio y rollback en cuatro ambientes Docker.

La integración previa permitió utilizar el sistema real. R6 se implementó sobre la base de datos, sin modificar el código del backend, frontend ni IAM para este reto. Las pruebas con puertos alternativos fueron ensayos de aprendizaje; los resultados por ambiente descritos aquí corresponden a los puertos documentados de la instalación.

Este informe reconstruye las actividades y resultados compartidos durante el trabajo. Los bloques de comandos identifican cómo se realizaron o consultaron las pruebas; **no constituyen una instrucción para repetir migraciones o rollbacks sobre el estado actual**.

> **Captura 1 — Presentación del trabajo:** agregar el enunciado del plan de mejora o la identificación del reto asignado.

## 2. Entorno utilizado

### Integración de la aplicación

| Componente | Ejecución | Puerto |
| --- | --- | ---: |
| PostgreSQL de develop | Docker | 15432 |
| IAM | Windows | 8081 |
| Backend Reference Data | Windows | 8080 |
| Microfrontend Reference Data | Vite en Windows | 5105 |
| Web-shell | Vite en Windows | 5000 |

### Ambientes de base de datos para R6

| Ambiente | Archivo de configuración | Proyecto Docker | Base de datos | Puerto |
| --- | --- | --- | --- | ---: |
| develop | `.env.develop` | ds-develop | design-software-develop | 15432 |
| qa | `.env.qa` | ds-qa | design-software-qa | 15433 |
| staging | `.env.staging` | ds-staging | design-software-staging | 15434 |
| main local | `.env.main` | ds-main | design-software-main | 15435 |

Se utilizaron ambientes Docker locales separados, no producción remota. Las pruebas sobre estos ambientes no equivalen a fusionar o promover la rama de implementación en Git.

> **Captura 2 — Entorno:** agregar la salida de Docker con nombres, puertos y estado de salud. Ocultar contraseñas y tokens en todas las evidencias.

## 3. Paso 1: comprobar la integración funcional previa

Antes de iniciar R6 se conectaron el login de IAM, la sesión y sus permisos, el backend de Reference Data y el microfrontend dentro del web-shell. Se corrigieron proxies, controles visuales de permisos y consultas de niveles hijos. También se incorporó el consumo del refresh token de IAM en el frontend.

Se comprobó el acceso real y la creación de registros desde la pantalla hasta PostgreSQL. Esta preparación permitió probar después el esquema migrado con la aplicación existente.

El recorrido utilizado fue:

```text
Formulario del MFE dentro del shell
                ↓
Petición HTTP con JSON y token
                ↓
Backend Reference Data: autorización y operación
                ↓
PostgreSQL: persistencia y consulta
                ↓
Respuesta y actualización del listado
```

El ciclo real completo de expiración y rotación del refresh token no se presenta aquí como medido. Los detalles de esta etapa están separados en [el informe de integración previo a R6](./Cambios%20locales%20de%20integraci%C3%B3n%20E2E%20de%20reference-data%20previos%20a%20R6.md).

> **Captura 3 — Integración:** agregar el MFE dentro del shell, una operación real y su fila correspondiente en pgAdmin.

## 4. Paso 2: preparar la rama y reconocer el esquema original

La implementación definitiva se realizó en `design-software-reference-data-db`, rama `hu-r6-dev`. La carpeta `design-software-reference-data-db-prueba` quedó como material de ensayo.

Se seleccionó la tabla real `institutional_structure.macroregion`. Su columna original era:

```sql
name VARCHAR(100) NOT NULL
```

Se revisaron los nombres existentes, el historial de Liquibase y las dependencias registradas sobre la columna. El objetivo fue evolucionar el almacenamiento conservando el nombre `name` que utiliza el backend.

La migración no aumenta por sí sola los límites aceptados por la API ni modifica la jerarquía del dominio.

> **Captura 4 — Estado inicial:** agregar la rama de Git, la definición original de la columna y el listado de datos anterior a la migración.

## 5. Paso 3: implementar la expansión

Se agregó una columna nueva sin retirar la anterior:

```sql
ALTER TABLE institutional_structure.macroregion
ADD COLUMN name_v2 TEXT;
```

**Changeset:** `970_r6_expand_macroregion_name`.

El backend continuó utilizando `name`. La columna temporal permitió preparar la nueva representación sin copiar ni eliminar registros en esta fase.

Antes de aplicar cambios se ejecutaron `validate`, `status --verbose` y `update-sql`. Al encontrar migraciones del equipo pendientes, se utilizaron filtros para evitar un update general no revisado. En develop, la expansión reportó un changeset aplicado.

> **Captura 5 — Expansión:** agregar el resumen de Liquibase y la consulta que muestra `name VARCHAR(100)` junto a `name_v2 TEXT`.

## 6. Paso 4: sincronizar nuevas escrituras mediante un trigger

Se creó la función `institutional_structure.sync_macroregion_name_v2()` y el trigger `trg_macroregion_sync_name_v2`.

**Changeset:** `980_r6_sync_macroregion_name`.

El trigger se ejecuta BEFORE INSERT OR UPDATE por cada fila y realiza:

```sql
NEW.name_v2 := NEW.name;
```

Así, una escritura del backend en `name` también completa `name_v2` dentro de la misma transacción. Esto no completa automáticamente los registros antiguos que no se modifican.

Se probaron INSERT y UPDATE con un registro temporal. La transacción de prueba terminó con ROLLBACK para no conservar ese registro. Ese ROLLBACK de datos no equivale a revertir el changeset de Liquibase.

> **Captura 6 — Trigger:** agregar el resultado de sincronización al crear y modificar, o el mensaje de la prueba automática, junto con la ausencia del registro temporal después del ROLLBACK.

## 7. Paso 5: ejecutar el backfill

**Changeset:** `990_r6_backfill_macroregion_name`.

Se completaron las filas existentes mediante:

```sql
UPDATE institutional_structure.macroregion
SET name_v2 = name
WHERE name_v2 IS DISTINCT FROM name;
```

La condición incluye valores NULL y evita actualizar filas que ya coinciden. La comprobación posterior fue:

```sql
SELECT COUNT(*) AS total,
       COUNT(*) FILTER (
           WHERE name_v2 IS DISTINCT FROM name
       ) AS pendientes
FROM institutional_structure.macroregion;
```

En develop se observaron inicialmente tres registros y cero pendientes, porque ya estaban sincronizados. Esa ejecución no demuestra una copia efectiva. En staging y main, ANT existía antes de la expansión; la fase reportó una fila afectada y después se comprobó la igualdad de ambos nombres, aportando evidencia efectiva del backfill.

El rollback de esta fase conserva la copia mediante `SELECT 1`. La columna temporal se elimina al revertir posteriormente la expansión; no se borran filas del dominio.

> **Captura 7 — Backfill:** agregar los datos anteriores, el resumen con una fila afectada de staging o main y la comprobación de igualdad posterior. No presentar únicamente cero pendientes como prueba de una copia efectiva.

## 8. Paso 6: ejecutar el contract

**Changeset:** `1000_r6_contract_macroregion_name`.

La retirada se registró después del DML, en `04_tcl/00_transaction_blocks`. Sus operaciones estructurales siguen siendo DDL. Liquibase administra su ejecución transaccional.

El procedimiento fue:

1. Limitar a tres segundos la espera para adquirir bloqueos.
2. Adquirir ACCESS EXCLUSIVE sobre la tabla.
3. Verificar dentro del bloqueo que no existan nombres sin sincronizar.
4. Retirar el trigger y su función.
5. Establecer NOT NULL en `name_v2`.
6. Eliminar la columna anterior y renombrar la nueva como `name`.

```text
Antes:   name VARCHAR(100) + name_v2 TEXT
Después: name TEXT NOT NULL
```

El resumen de aplicación reportó un changeset ejecutado. Se comprobó el tipo TEXT y la conservación de las macrorregiones.

> **Captura 8 — Contract:** agregar su ejecución y la consulta de `information_schema.columns` que muestra el esquema final.

## 9. Paso 7: comprobar la aplicación con el esquema final

Después del contract se consultó el listado desde el web-shell y se creó la macrorregión de código `PAS` desde la pantalla. El registro apareció tanto en el listado como en PostgreSQL con `name TEXT`.

Esta prueba confirma lectura y creación con el esquema final, usando el servicio existente. La aplicación ya no necesita el trigger porque solo queda la columna final `name`.

La comprobación funcional antes y después no mide disponibilidad durante la ejecución. ACCESS EXCLUSIVE bloquea temporalmente lecturas y escrituras; no se atribuyen resultados de latencia, cero errores o ausencia total de caída sin mediciones concurrentes.

> **Captura 9 — Compatibilidad:** agregar la creación de PAS desde pantalla y la consulta de la misma fila en PostgreSQL. La modificación desde pantalla en el esquema final debe identificarse por separado si se aporta esa evidencia.

## 10. Paso 8: probar la reversión en los ambientes Docker

Antes de cada reversión se revisó el SQL completo para confirmar qué changesets serían afectados. En staging y main se separaron explícitamente las fases con estos filtros:

```text
Expansión, trigger y backfill:
--label-filter="R6 AND !contract" --context-filter="01_ddl,02_dml"

Contract:
--label-filter="R6 AND contract" --context-filter="04_tcl,r6-contract"
```

El rollback completo de R6 ejecuta el orden inverso: `1000 → 990 → 980 → 970`. El rollback del contract reconstruye ambas columnas y el trigger; rechaza nombres mayores de 100 caracteres en lugar de truncarlos.

| Ambiente | Prueba realizada | Resultado observado |
| --- | --- | --- |
| develop | Contract y retorno al esquema ampliado. | Cuatro macrorregiones conservadas, cero pendientes y outbox reconstruida tras la incidencia indicada abajo. La repetición aislada aún no tiene una salida concluyente de reversión efectiva. |
| qa | Rollback completo hasta `r6_qa_before_expand`. | Cuatro changesets R6 revertidos; volvió `name VARCHAR(100)` y ANT permaneció presente. No se capturó un listado completo previo para comparar todas las filas. |
| staging | Instalación inicial sin R6, aplicación por fases y rollback hasta `r6_staging_before_expand`. | Se revirtieron los cuatro cambios; mismo UUID, código y nombre de ANT, y outbox conservada. |
| main local | Instalación inicial sin R6, aplicación por fases y rollback hasta `r6_main_before_expand`. | Se revirtieron los cuatro cambios; mismo UUID, código y nombre de ANT, y outbox conservada. |

En staging y main, la instalación inicial aplicó 18 changesets sin R6; después se aplicaron tres de expansión/sincronización/backfill y uno de contract. Al finalizar sus pruebas, ambos ambientes quedaron nuevamente con el esquema original de la columna.

> **Captura 10 — Develop:** agregar historial y esquema recuperado; no usar una salida de cero cambios como prueba de una reversión efectiva nueva.

> **Captura 11 — Qa:** agregar los cuatro changesets revertidos y el esquema y datos posteriores.

> **Captura 12 — Staging:** agregar listado previo, backfill, rollback, mismo registro ANT y existencia de outbox.

> **Captura 13 — Main local:** agregar listado previo, backfill, rollback, mismo registro ANT y existencia de outbox.

### Incidencias y recuperación

- **Develop:** una etiqueta anterior también incluyó cuatro changesets ajenos a R6. Se retiraron outbox, una fila demo y permisos. Los cuatro cambios se reaplicaron y se confirmó la conservación de las macrorregiones y la reconstrucción de outbox. No se pudo determinar si había eventos anteriores en esa tabla ni recuperar posibles filas eliminadas. La etiqueta de historial no es un respaldo.
- **Qa:** la selección de solo R6 aplicó también el contract. Las fases posteriores en staging y main se seleccionaron con los filtros explícitos anteriores; no se considera demostrada la protección del contexto por sí sola.
- **Staging:** se resolvió un error de conexión `UnknownHostException: postgres` iniciando el PostgreSQL del ambiente. La validación posterior fue exitosa.

> **Captura 14 — Recuperación:** agregar los cuatro cambios reaplicados en develop y la comprobación de cuatro registros, cero pendientes y outbox existente.

## 11. Paso 9: publicar la implementación y documentar la decisión

Los cambios se publicaron en `hu-r6-dev` mediante commits pequeños con mensajes en inglés:

| Commit | Propósito |
| --- | --- |
| `cc89fb4` | Expansión y rollback. |
| `8205610` | Trigger de sincronización. |
| `4346d21` | Backfill. |
| `947d03f` | Contract y rollback. |
| `64052c3` | Traducción de comentarios descriptivos de changelogs. |
| `64dd0bf` | Inclusión del SQL de rollback del trigger que faltaba en la versión confirmada en Git. |

La documentación y las evidencias se guardan en `Docs-reference-data`, separadas del repositorio de implementación. La decisión técnica está en el [ADR-014](./adr/ADR-014-macroregion-name-expand-contract.md), con numeración propuesta y estado PROPOSED.

> **Captura 15 — Entrega:** agregar historial de commits, rama publicada y enlace al PR cuando se haya creado. Una rama publicada no equivale a un PR fusionado.

## 12. Resultados y mediciones disponibles

| Indicador | Resultado registrado | Alcance |
| --- | --- | --- |
| Sincronización en develop tras recuperación | 4 registros, 0 pendientes. | Estado ampliado conservado. |
| Backfill efectivo en staging y main | 1 fila afectada en cada fase de expansión/backfill; ANT sincronizada. | Datos iniciales de demostración. |
| Aplicación por fases en staging y main | 3 changesets y después 1. | Separación explícita de fases. |
| Rollback completo en qa, staging y main | 4 changesets revertidos por ambiente. | SQL y resultados posteriores compartidos. |
| Conservación de ANT en staging y main | Mismo UUID, código y nombre antes y después. | UUID `a0000000-0000-0000-0000-000000000001`. |
| Latencias HTTP, errores durante el cambio y duración del bloqueo | Sin medición registrada. | No se inventan cifras ni se certifica disponibilidad concurrente. |

## 13. Cierre y alcance de la evidencia

Se implementaron y publicaron las cuatro fases de R6. Las pruebas muestran sincronización, copia efectiva de datos existentes en staging y main, funcionamiento de lectura y creación con el esquema final y reversión completa en qa, staging y main.

La evidencia de develop debe cerrar su repetición aislada y conservar el registro de la incidencia. La aceptación de disponibilidad requiere medir solicitudes durante la migración y verificar las condiciones de cancelación. La revisión del PR y la sustentación individual son controles adicionales a la ejecución local.

## Referencias

- [Rama de implementación hu-r6-dev](https://github.com/code-sena/design-software-reference-data-db/tree/hu-r6-dev).
- [ADR-014: decisión de evolución del nombre](./adr/ADR-014-macroregion-name-expand-contract.md).
- [Resumen para Arquitectura](./R6%20-%20Implementation%20overview%20for%20architects.md).
- [Guía de ejecución local](./Ejecucion-base-de-datos.md).

### Cómo agregar las imágenes

Guarda cada captura con un nombre identificable dentro de una carpeta de evidencias y reemplaza el bloque de captura correspondiente por un enlace relativo. Ejemplo de formato:

```markdown
![Expansión: columnas name y name_v2](evidence/r6/05-expansion-columnas.png)
```

Ese nombre es un ejemplo; no indica que la imagen ya exista. Mantén visibles el ambiente, la consulta o comando y el resultado, sin exponer credenciales.
