# Informe técnico del trabajo realizado: integración de Reference Data y R6

**Responsable:** María José Rodríguez Oyola  
**Dominio:** Reference Data  
**Repositorio de implementación de R6:** `design-software-reference-data-db`  
**Rama de implementación:** `hu-r6-dev`

## 1. Integración local del dominio

Preparé la ejecución local del dominio conectando PostgreSQL en Docker, IAM, el backend Reference Data, su microfrontend y el web-shell. El objetivo fue utilizar operaciones reales desde la pantalla y comprobar su persistencia en PostgreSQL.

La aplicación utilizó los puertos 15432 para PostgreSQL de develop, 8081 para IAM, 8080 para Reference Data, 5105 para el microfrontend y 5000 para el web-shell.

| Repositorio o componente | Trabajo realizado y finalidad |
| --- | --- |
| `design-software-docker-infra-develop` | Configuración local de ambientes, conexiones y puertos, y preparación del montaje del repositorio de base de datos para ejecutar Liquibase. |
| `design-software-web-shell` | Login real mínimo, proxies hacia IAM y Reference Data, manejo de sesión y entrega de permisos reales al microfrontend. |
| `design-software-frontend-platform` | Ajustes del cliente de autenticación y del proveedor de tokens para login, consulta de identidad, renovación mediante refresh token y limpieza de sesión. |
| `design-software-reference-data-mfe` | Conexión HTTP con token, solicitud de renovación ante 401, habilitación de controles según permisos y ajustes de consulta de niveles hijos sin selección previa del padre. |
| `design-software-iam-db` y `design-software-iam-service` | Uso de migraciones y servicio existentes para preparar la autenticación local. No se modificó su lógica para R6. |
| `design-software-reference-data-service` | Preparación y ejecución del backend existente, configurando PostgreSQL y validación de tokens mediante IAM. Se reutilizó su código durante R6. |
| `design-software-design-system` | Dependencia visual compartida. No se atribuye una modificación funcional propia para R6. |
| `design-software-reference-data-db-prueba` | Ensayos de aprendizaje antes de trasladar la implementación al repositorio definitivo. |
| `Docs-reference-data` | Documentación del trabajo, decisión técnica y resultados de las pruebas. |

Los principales archivos de integración identificados fueron:

- **Web-shell:** `vite.config.ts`, `src/app/LoginPage.tsx`, `src/app/login.css`, `src/app/ShellRoot.tsx`, `src/main.tsx`, `src/shell/remote/RemoteRoutes.tsx` y los adaptadores de sesión de `src/app/data-source/`.
- **Frontend-platform:** `src/auth-client/AuthClient.ts` y `src/http-client/HttpClient.ts`, junto con pruebas asociadas.
- **Microfrontend:** `vite.config.ts`, `src/adapter/out/http/referenceDataHttp.ts`, `src/contracts/remote-props.ts`, `src/App.tsx`, `src/adapter/in/components/GeographyHierarchy.tsx`, hooks de microrregiones y departamentos, sus servicios HTTP y tipos de consulta.
- **Infraestructura:** archivos locales `.env.develop`, `.env.qa`, `.env.staging`, `.env.main` y configuración de montaje de Liquibase en `docker-compose.yml`.

Se comprobó el inicio de sesión real, la carga del microfrontend dentro del shell y el registro y consulta de datos desde la interfaz hasta PostgreSQL. Los permisos visuales no sustituyeron la autorización del backend.

La incorporación de refresh token y las correcciones de proxies, sesión y listados corresponden a esta integración previa, no a la migración R6. El ciclo real completo de expiración y rotación no se presenta como medido. Tampoco se atribuye al trabajo la creación completa de los componentes existentes.

## 2. Implementación del reto R6

Mi trabajo individual corresponde al **reto 13 — R6: evolución de esquema sin caída**, sobre la tabla real `institutional_structure.macroregion`.

Se evolucionó `name VARCHAR(100) NOT NULL` a `name TEXT NOT NULL`, utilizando temporalmente `name_v2` para conservar el contrato del backend durante las etapas.

```text
Original:    name VARCHAR(100) NOT NULL
Transición:  name VARCHAR(100) NOT NULL + name_v2 TEXT
Final:       name TEXT NOT NULL
```

El cambio demuestra el procedimiento de evolución solicitado; no amplía automáticamente la longitud admitida por las validaciones actuales de la API.

| Etapa | Changeset | Implementación |
| --- | --- | --- |
| Expansión | `970_r6_expand_macroregion_name` | Agrega `name_v2 TEXT`, inicialmente nullable, conservando la columna original y sus registros. |
| Sincronización | `980_r6_sync_macroregion_name` | Crea una función y un trigger BEFORE INSERT OR UPDATE que copian `name` a `name_v2`. |
| Backfill | `990_r6_backfill_macroregion_name` | Copia los valores existentes únicamente donde las columnas difieren, incluyendo NULL. |
| Contract | `1000_r6_contract_macroregion_name` | Verifica sincronización, elimina trigger y función, conserva NOT NULL y reemplaza la columna anterior manteniendo el nombre final `name`. |

Se agregaron estos archivos al repositorio de base de datos:

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

Se modificaron los archivos `0000changelog.yaml` de las cuatro ubicaciones de migración para registrar los SQL y sus rollbacks. También se tradujeron comentarios descriptivos de changelogs al inglés, sin cambiar operaciones SQL ni datos del dominio.

El contract se ejecuta en una transacción. Comprueba los datos bajo ACCESS EXCLUSIVE y limita a tres segundos la espera para adquirir bloqueos. Este límite no corresponde a la duración total de la migración.

Su rollback reconstruye ambas columnas y el trigger con los nombres actuales. Si algún nombre supera 100 caracteres, cancela sin truncarlo. El rollback del backfill conserva los valores copiados; la columna temporal se retira al revertir la expansión.

## 3. Pruebas y resultados registrados

Se probó la sincronización del trigger al insertar y modificar un registro temporal. Sus datos se deshicieron con un rollback de transacción, distinto del rollback de Liquibase.

En staging y main, ANT existía antes de la expansión. La fase de expansión y backfill reportó una fila afectada; posteriormente se comprobó que `name` y `name_v2` coincidían. Esto aporta evidencia de copia efectiva de datos existentes.

Después del contract se comprobó `name TEXT` y se creó PAS desde la pantalla. El registro apareció en PostgreSQL y en el listado del microfrontend. El rollback posterior del contract en develop conservó cuatro macrorregiones, incluida PAS.

| Ambiente Docker local | Puerto | Resultado observado |
| --- | ---: | --- |
| develop | 15432 | Contract aplicado y retorno al esquema ampliado, conservando cuatro macrorregiones. |
| qa | 15433 | Reversión de los cuatro changesets R6; recuperación de `name VARCHAR(100)` y presencia de ANT. |
| staging | 15434 | Aplicación por fases y rollback completo; ANT conservó UUID, código y nombre, y outbox permaneció presente. |
| main local | 15435 | Aplicación por fases y rollback completo; ANT conservó UUID, código y nombre, y outbox permaneció presente. |

En develop, una etiqueta antigua también revirtió cuatro changesets ajenos a R6. Se reaplicaron y se reconstruyeron sus objetos y permisos; las macrorregiones se conservaron. No se determinó si outbox contenía eventos anteriores. La repetición aislada de develop todavía requiere evidencia concluyente de ejecución.

Las pruebas comprobaron funcionamiento antes y después de los cambios. No se registraron mediciones concurrentes de disponibilidad durante el bloqueo del contract; por tanto, no se atribuye a esas pruebas una demostración de ausencia de interrupciones bajo carga.

Los ambientes corresponden a instancias Docker locales separadas, no a producción remota ni a una promoción de ramas Git. La implementación definitiva se publicó en `hu-r6-dev` mediante los commits `cc89fb4`, `8205610`, `4346d21`, `947d03f`, `64052c3` y `64dd0bf`.

Los cambios de integración se conservaron como evidencia local. Su incorporación a un repositorio de evidencias no implica una fusión en los repositorios oficiales de cada componente.

## 4. Documentación y evidencias

- **ADR e informe técnico:** https://github.com/MariaOyola/Docs-reference
- **Código y evidencias del trabajo:** https://github.com/MariaOyola/Reference-data-evidencia
