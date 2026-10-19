# Archivos por capas

Guía breve para entender Reference Data. Primero se explica el frontend; backend y base de datos se completarán después en este mismo documento.

## 1. Frontend

El frontend muestra formularios, recoge los datos y hace peticiones al backend. No se conecta directamente a PostgreSQL.

### Los cuatro proyectos

| Proyecto | Responsabilidad |
| --- | --- |
| `design-software-web-shell` | Aplicación principal: login, menú, navegación y montaje de módulos. |
| `design-software-reference-data-mfe` | Pantallas de geografía, centros, catálogos y parámetros. |
| `design-software-frontend-platform` | Herramientas compartidas de sesión, autenticación y HTTP. |
| `design-software-design-system` | Estilos y componentes visuales compartidos. |

No son cuatro pasos consecutivos: el shell utiliza las bibliotecas compartidas y carga el MFE dentro de su pantalla.

### Web-shell: contiene tu módulo

Rutas dentro de `design-software-web-shell/`:

| Archivo | Qué hace |
| --- | --- |
| `src/main.tsx` | Arranca React, prepara `AuthClient`, configura el almacenamiento de tokens y atiende las solicitudes de renovación del MFE. |
| `src/app/LoginPage.tsx` | Recoge correo y contraseña y solicita iniciar sesión. |
| `src/app/ShellRoot.tsx` | Coordina la sesión y muestra el contenido autenticado. |
| `src/shell/remote/RemoteRoutes.tsx` | Monta el módulo de la ruta y entrega los permisos del usuario a Reference Data. |
| `vite.config.ts` | Configura el puerto 5000 y los proxies de desarrollo hacia IAM y Reference Data. |

Al visitar `/reference-data`, el shell muestra tu MFE dentro de la navegación principal.

### Reference Data MFE: formulario y operaciones

Ejemplo: registrar una macrorregión llamada **Región Antioqueña**.

Rutas dentro de `design-software-reference-data-mfe/`:

| Archivo y función | Responsabilidad y conexión |
| --- | --- |
| `src/App.tsx` — `App`, `onSave` | Recibe permisos del shell, conecta las pantallas y dirige el guardado a la operación del nivel correspondiente. |
| `src/adapter/in/components/GeographyHierarchy.tsx` — `GeographyHierarchy` | Muestra tabla y formulario; recoge nombre, código y estado, valida campos y llama a `onSave`. |
| `src/application/useMacroregions.ts` — `useMacroregions`, `create` | Recibe los datos, llama al servicio HTTP y solicita recargar el listado tras guardar. Controla carga, errores y paginación. |
| `src/adapter/out/http/macroregionsService.ts` — `macroregionsService.create` | Prepara el POST a `macroregions` con JSON y llama a `requestReferenceData`. Su función `list` prepara el GET del listado. |
| `src/adapter/out/http/referenceDataHttp.ts` — `requestReferenceData` | Ejecuta `fetch`, adjunta el token actual y procesa respuestas o errores. Ante 401 solicita al shell renovar la sesión y reintenta una vez si la renovación funciona. |
| `src/domain/geography.ts` | Define las estructuras de datos: macrorregión, datos de creación y paginación. No guarda registros. |

Organización interna:

```text
adapter/in  → entrada: pantallas y formularios
application → coordinación: consultar, crear y actualizar la pantalla
adapter/out → salida: peticiones al backend
domain      → estructuras de datos utilizadas por el frontend
```

El servicio HTTP del frontend prepara peticiones. La lógica de negocio del servidor y la autorización están en el backend.

### Frontend-platform: sesión y comunicación compartidas

Rutas dentro de `design-software-frontend-platform/`:

| Archivo o carpeta | Qué hace |
| --- | --- |
| `src/auth-client/AuthClient.ts` — `AuthClient` | Solicita login a IAM, consulta la identidad, coordina renovación y cierre de sesión. IAM comprueba las credenciales y emite los tokens. |
| `src/http-client/HttpClient.ts` — `HttpClient` | Ejecuta las peticiones de sus consumidores, adjunta el token mediante su proveedor y maneja errores y reintentos. |
| `src/contracts/` | Contiene contratos y tipos compartidos para acordar qué datos intercambian los proyectos. |

En la integración actual, el MFE usa su propio `referenceDataHttp.ts`, no llama directamente a `HttpClient`. Lee el token de sessionStorage del origen del shell y solicita la renovación con el evento interno `sena:refresh-session`. El shell delega esa renovación en `AuthClient`.

### Design-system: presentación compartida

Rutas dentro de `design-software-design-system/`:

| Archivo o carpeta | Qué aporta |
| --- | --- |
| `src/tokens/` | Colores, tipografía, espacios y otros valores visuales. |
| `src/components/` | Componentes visuales reutilizables. |
| `src/index.ts` | Expone los elementos que otros proyectos pueden importar. |

El shell importa sus tokens mediante `design-software-web-shell/src/styles/tokens.css`. El formulario geográfico utiliza React y estilos propios del MFE; no todos sus botones proceden del design-system.

Design-system no guarda datos, no genera tokens y no comprueba permisos.

### Recorrido del frontend: guardar y volver a mostrar

```text
Shell monta Reference Data y entrega permisos
                    ↓
GeographyHierarchy recoge y valida los campos
                    ↓
App.onSave elige la operación
                    ↓
useMacroregions.create coordina el guardado
                    ↓
macroregionsService.create prepara el POST
                    ↓
requestReferenceData envía JSON + token
                    ↓
Proxy del shell dirige la petición al backend
                    ↓
Backend procesa y devuelve la respuesta
                    ↓
useMacroregions solicita actualizar el listado
                    ↓
macroregionsService.list prepara un GET
                    ↓
La respuesta actualiza el estado y la tabla se vuelve a dibujar
```

En desarrollo, el navegador solicita `/reference-data-api/macroregions` al shell; su proxy elimina `/reference-data-api` y dirige la petición a `http://localhost:8080/macroregions`.

**Diferencia esencial:** el formulario valida campos y puede deshabilitar botones según permisos; el backend decide si autoriza la operación; PostgreSQL aplica sus restricciones al guardar.

## 2. Backend

### Los tres proyectos

| Proyecto | Responsabilidad |
| --- | --- |
| `design-software-iam-service` | Autentica usuarios, obtiene sus roles y permisos asignados, emite y renueva tokens. |
| `design-software-reference-data-service` | Autoriza operaciones del dominio, procesa sus reglas y consulta o guarda sus datos. |
| `design-software-docker-infra-develop` | Infraestructura local: define PostgreSQL y herramientas para aplicar migraciones. No procesa formularios. |

### IAM: organización

Rutas dentro de `design-software-iam-service/`:

| Carpeta o archivo | Función |
| --- | --- |
| `iam-api/` | Arranque y configuración del servicio Spring Boot. |
| `iam-adapters/` | Adaptadores HTTP, persistencia y herramientas externas. |
| `iam-core/` | Casos de uso, dominio y contratos necesarios para ejecutar las operaciones. |
| `contracts/` | Contrato de solicitudes y respuestas de la API. |
| `iam-api/src/main/java/co/sena/iam/api/IamApiApplication.java` — `main` | Inicia Spring Boot. |
| `iam-adapters/src/main/java/co/sena/iam/adapter/in/rest/AuthController.java` — `login` | Recibe la solicitud de login, delega la autenticación al caso de uso y devuelve los tokens. |

IAM no crea permisos arbitrarios al iniciar sesión: utiliza las asignaciones del usuario. El JWT es una credencial temporal; no contiene la contraseña. Los 900 segundos del access token equivalen a 15 minutos.

### Reference Data: organización

Rutas dentro de `design-software-reference-data-service/`:

| Carpeta | Responsabilidad |
| --- | --- |
| `cmd/reference-data-api/` | Arranque y conexión de los componentes. |
| `internal/adapter/in/` | Entrada HTTP y seguridad. |
| `internal/application/` | Coordinación de operaciones y reglas de aplicación. |
| `internal/domain/` | Conceptos y reglas del dominio. |
| `internal/adapter/out/` | Comunicación con PostgreSQL y otras salidas. |

`cmd/reference-data-api/main.go`, función `main`, prepara el validador de tokens, abre la conexión a PostgreSQL, crea el repositorio y los servicios, registra las rutas e inicia el servidor HTTP. Conecta las piezas; no concentra toda la lógica de creación.

### Responsabilidades durante una creación

Ejemplo: registrar la macrorregión **Región Antioqueña**.

| Parte | Qué hace |
| --- | --- |
| Seguridad | Valida el token y exige el permiso de la operación antes de permitir que continúe. |
| Handler HTTP | Interpreta el JSON y prepara la llamada al servicio; transforma el resultado en una respuesta HTTP. |
| Servicio de aplicación | Coordina la creación y las comprobaciones que le corresponden; llama al repositorio. |
| Repositorio | Ejecuta SQL para persistir o consultar datos. |
| PostgreSQL | Conserva los registros y aplica las restricciones SQL. |

Un JSON correcto no garantiza autorización. La petición HTTP incluye método, ruta, cabeceras y cuerpo; el JSON es el formato del cuerpo, no de toda la petición.

- Token inválido o vencido: `401`.
- Token válido sin el permiso requerido: `403`.
- Token y permiso válidos: puede continuar; todavía debe superar las validaciones de datos y restricciones SQL.

El backend comprueba el permiso requerido y su alcance; el nombre del rol, como SYSTEM_ADMIN o Aprendiz, no sustituye esa comprobación.

### Ruta y seguridad de POST /macroregions

**1. ¿Dónde se conecta la ruta con el backend?**

En `design-software-reference-data-service/cmd/reference-data-api/main.go`, la función `registerRoutes` conecta las rutas del dominio con la seguridad:

```go
security.RegisterProtectedRoutes(mux, validator, hierarchyHandler.Routes())
```

Recibe el enrutador HTTP, el validador de tokens y el handler. Registra cómo atender las peticiones; todavía no guarda ningún dato.

**2. ¿Dónde se define POST /macroregions?**

En `design-software-reference-data-service/internal/adapter/in/http/hierarchy/hierarchy.go`, la función `Handler.Routes` declara las operaciones disponibles:

| Elemento | Valor |
| --- | --- |
| Método | `POST` |
| Ruta | `/macroregions` |
| Permiso exigido | `REF_HIERARCHY_MANAGE` |
| Función que atenderá la petición | `h.create("macroregions", w, r)` |

El mismo código registra operaciones similares para microrregiones, departamentos y municipios. Por eso no es necesario buscar un archivo llamado `MacroregionController`.

**3. ¿Quién impide que una petición sin permiso llegue al handler?**

`RegisterProtectedRoutes` envuelve cada ruta con `RequireFeature`. Estas son las responsabilidades de los archivos involucrados:

Rutas dentro de `design-software-reference-data-service/`:

| Archivo y función | Qué recibe, qué hace y a quién llama |
| --- | --- |
| `cmd/reference-data-api/main.go` — `registerRoutes` | Recibe enrutador, validador y handler. Obtiene las rutas con `hierarchyHandler.Routes()` y las entrega a `security.RegisterProtectedRoutes`. |
| `internal/adapter/in/http/hierarchy/hierarchy.go` — `Handler.Routes` | Define método, ruta, permiso y función para cada operación. Para crear macrorregiones conecta POST `/macroregions`, permiso `REF_HIERARCHY_MANAGE` y `h.create("macroregions", w, r)`. No guarda datos. |
| `internal/adapter/in/http/security/routes.go` — `RegisterProtectedRoutes` | Recibe las rutas y las registra en el enrutador, envolviendo cada handler con `RequireFeature`. |
| `internal/adapter/in/http/security/security.go` — `RequireFeature` | Recibe validador, permiso y siguiente handler. Comprueba la autorización de la petición; si pasa, permite ejecutar el handler. |

Fragmento real que conecta seguridad y handler:

```go
RequireFeature(validator, route.Feature, route.Handler)
```

Se interpreta así: “Usa este validador, exige el permiso de esta ruta y, si la comprobación pasa, permite ejecutar el handler”.

Para crear **Región Antioqueña**, el recorrido hasta aquí es:

```text
POST /macroregions + JSON + token
                 ↓
El enrutador encuentra la ruta
                 ↓
RequireFeature comprueba token y permiso
                 ↓
Si pasa, ejecuta h.create("macroregions", ...)
```

Registrar las rutas ocurre al arrancar el servidor. Comprobar el token y el permiso ocurre al recibir cada petición.

**Recordatorio:** `Handler.Routes` define funciones y permisos; no guarda la macrorregión. Más adelante, el repositorio envía la instrucción SQL y PostgreSQL la ejecuta, aplica sus restricciones y conserva el registro.

### Infraestructura local

Rutas dentro de `design-software-docker-infra-develop/`:

| Archivo | Función |
| --- | --- |
| `docker-compose.yml` | Define servicios, puertos, volúmenes y herramientas Liquibase. |
| `.env.develop` | Proporciona valores de configuración del entorno local. |

En la ejecución actual, PostgreSQL funciona en Docker: el puerto 15432 de Windows llega al 5432 del contenedor. El volumen `postgres_data` conserva sus archivos entre reinicios. Liquibase aplica los cambios definidos en los repositorios de base de datos. IAM y Reference Data se ejecutan en Windows.

### Pendiente de estudiar en detalle

### Handler.create y servicio de aplicación

Ejemplo: POST `/macroregions`, cuerpo `{"name":"Región Antioqueña","code":"ANTQ"}`. La seguridad ya permitió continuar; el handler no vuelve a comprobar la contraseña.

En `design-software-reference-data-service/internal/adapter/in/http/hierarchy/hierarchy.go`:

- `Handler.create` recibe el recurso (`macroregions`) y la petición HTTP. Llama a `commandFromRequest(name, r, true)`; true indica creación.
- `commandFromRequest` interpreta el JSON, rechaza campos desconocidos y construye un `application.Command`. Para macrorregiones exige nombre y código no vacíos y comprueba los límites implementados: 100 para el nombre y 10 para el código. En Go estas comprobaciones utilizan `len` de cadenas, es decir, longitud en bytes. Una solicitud inválida se rechaza antes de llamar al servicio.
- Si la interpretación es válida, `Handler.create` llama a `h.commands.Create(r.Context(), name, command)`. Ante éxito responde con `writeJSON(w, http.StatusCreated, item)`: HTTP 201 y el registro creado. También transforma errores en respuestas HTTP.

En `design-software-reference-data-service/internal/application/hierarchy/query.go`, `CommandService.Create` recibe contexto, recurso y comando. Para macrorregiones llama a `s.writer.Create(ctx, resource, command)` y devuelve su resultado o error. El writer fue conectado al repositorio PostgreSQL durante el arranque. No escribe SQL directamente ni vuelve a validar el JWT. En este caso concreto, las comprobaciones de formato y campos vistas están en `commandFromRequest`, no todas en el servicio. Aunque el archivo se llama query.go, también contiene CommandService.

```text
Handler.create
  → commandFromRequest interpreta y valida los datos
  → CommandService.Create coordina la creación
  → writer.Create delega la persistencia al repositorio
  → resultado o error vuelve al handler
  → HTTP 201 + JSON si se creó correctamente
```

### Pendiente de estudiar en detalle (continuación)

- Servicio de aplicación y repositorio: creación, SQL y transacción.
- Lectura: GET, consulta y respuesta al frontend.
- Detalles de IAM: BCrypt, firma JWT y validación mediante JWKS.

## 3. Base de datos

Pendiente de completar después del backend: las nueve tablas del dominio, sus relaciones y restricciones.
