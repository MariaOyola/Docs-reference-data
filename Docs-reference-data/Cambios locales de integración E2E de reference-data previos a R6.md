# Cambios locales de integración E2E de reference-data previos a R6

Fecha de reconstrucción: 20 de octubre de 2026.

**Este informe documenta la preparación funcional del dominio antes de R6. No constituye evidencia de cumplimiento del reto R6.** Su propósito es explicar qué faltaba para conectar los componentes existentes, qué se agregó o corrigió y cómo se verificó el recorrido real.

## 1. Alcance y fuentes

El alcance es la conexión entre PostgreSQL, IAM, backend Reference Data, microfrontend y web-shell. Se incluyen login, tokens, permisos, consultas y operaciones desde la pantalla hasta las tablas existentes.

La reconstrucción utiliza:

- La conversación anterior al inicio de R6: autorización para crear el login real, problemas de permisos, registros creados desde la pantalla, consultas en pgAdmin, expiración de sesión y consultas de niveles hijos.
- `Seguimiento_E2E_reference-data.md`, conservado en la versión anterior a R6. Es un seguimiento acumulativo: sus apartados más antiguos pueden decir «pendiente» aunque posteriormente haya una confirmación en la conversación.
- `Docs-reference-data/Ejecucion-base-de-datos.md` y `Docs-reference-data/Archivos por capas.md`.
- Los commits `159546a`, `93d1685` y `5c45951`.

| Referencia | Fecha local registrada en Git | Qué permite comprobar |
| --- | --- | --- |
| `6dc7cc3` | 17/10/2026 | El commit inicial contiene únicamente el README raíz; no es una copia completa del código previo a la integración. |
| `159546a` | 18/10/2026 | Primera incorporación amplia de los proyectos y del seguimiento E2E. Contiene tanto componentes existentes como trabajo de integración; no todo archivo agregado fue creado por esta intervención. |
| `93d1685` | 18/10/2026 | Cambios identificables de renovación de sesión, consulta de niveles hijos y documentación de ejecución. |
| `5c45951` | 19/10/2026 | Última versión confirmada antes del inicio de R6: documentación por capas, comentarios, ajuste de TypeScript y limpieza de una copia duplicada del MFE. |

**Límite de atribución:** cuando Git incorpora un proyecto completo, no permite por sí solo distinguir qué código ya existía y qué se escribió durante la integración. En esos casos se utiliza el seguimiento y la conversación. Si no existe evidencia suficiente, se indica «pendiente de verificar».

## 2. Arquitectura local que se dejó conectada

| Componente | Ejecución anterior a R6 | Responsabilidad |
| --- | --- | --- |
| PostgreSQL | Docker, `localhost:15432` | Base `design-software-develop`, con esquemas separados para IAM y Reference Data. |
| IAM | Windows, `localhost:8081` | Login, emisión y renovación de tokens, identidad y permisos. |
| Backend Reference Data | Windows, `localhost:8080` | Autorizar operaciones y consultar o guardar datos del dominio. |
| Microfrontend Reference Data | Vite, `localhost:5105` | Formularios y listados del dominio; publicación del módulo federado. |
| Web-shell | Vite, `localhost:5000` | Login, sesión, navegación y montaje del microfrontend. |

```text
Navegador / web-shell :5000
  ├─ carga el microfrontend desde :5105
  ├─ /api/v1/auth/* → IAM :8081
  └─ /reference-data-api/* → Reference Data :8080
                                  ↓
                         PostgreSQL :15432
```

El proxy elimina `/reference-data-api` antes de enviar la petición al backend. Las rutas propias de Reference Data no necesitan que se agregue `/api/v1`.

Esta es una ejecución local mixta: PostgreSQL en Docker y servicios e interfaces en Windows. **No demuestra por sí misma el arranque completo mediante un perfil Docker del dominio.** La autorización del instructor para esa modalidad se registra como información aportada por la estudiante; su soporte formal queda pendiente de verificar.

## 3. Qué faltaba y qué se agregó o corrigió

### 3.1 Comunicación entre navegador, IAM y Reference Data

Faltaba asegurar que las peticiones del frontend llegaran al servicio correcto y que la navegación del shell no se confundiera con una ruta de API.

| Archivo | Cambio de integración y justificación |
| --- | --- |
| `design-software-web-shell/vite.config.ts` | Proxy específico de `/api/v1/auth` hacia IAM en `8081`, antes del proxy general; proxy `/reference-data-api` hacia `8080` con eliminación del prefijo. Se conservaron los proxies existentes y el puerto estricto `5000`. |
| `design-software-reference-data-mfe/vite.config.ts` | Proxy equivalente para uso independiente y puerto estricto `5105`. No se atribuye a esta intervención toda la configuración de federación que ya contenía el proyecto. |
| `design-software-reference-data-mfe/.env.local` | Base de API `VITE_REFERENCE_DATA_API_BASE_URL=/reference-data-api`. Es configuración local potencialmente ignorada por Git: su contenido histórico se respalda en el seguimiento, no en un archivo versionado. |
| `design-software-web-shell/.env.development` | Uso de sesión HTTP, desactivación de sesión local y uso de federación. La existencia de la configuración local se documentó en el seguimiento. |
| `design-software-reference-data-mfe/src/adapter/out/http/referenceDataHttp.ts` | Peticiones relativas al origen de la página, manejo de `Headers`, envío del token vigente y tratamiento de respuestas y errores. |

El MFE federado ejecuta su código dentro del origen del shell. Por eso puede leer la sesión de `5000`, aunque su JavaScript se descargue de `5105`. Abrir el MFE solo en otra pestaña de `5105` no comparte automáticamente ese `sessionStorage`.

### 3.2 Login real dentro del shell

No se disponía de `iam-mfe` para completar el acceso. Con autorización explícita de la usuaria se agregó un formulario real mínimo en el shell, utilizando el IAM existente.

| Archivo | Responsabilidad incorporada o ajustada |
| --- | --- |
| `design-software-web-shell/src/app/LoginPage.tsx` y `src/app/login.css` | Formulario de correo y contraseña, estado de envío y mensajes de error. |
| `design-software-web-shell/src/app/ShellRoot.tsx` | Login público fuera de la protección de sesión y retorno a una ruta interna después de autenticarse. |
| `design-software-web-shell/src/app/data-source/session-data-source.ts` | Contrato para la operación de login. |
| `design-software-web-shell/src/app/data-source/http-session-data-source.ts` | Adaptación del login del shell al cliente de autenticación. |
| `design-software-web-shell/src/app/hooks/use-current-user.ts` | Manejo de sesión ausente cuando IAM rechaza el token. |
| `design-software-frontend-platform/src/auth-client/AuthClient.ts` | Envío a `POST /api/v1/auth/login`, validación de la respuesta y consulta posterior de `/api/v1/auth/me`. Limpieza de sesión si la identidad no puede validarse. |
| `design-software-frontend-platform/src/http-client/HttpClient.ts` | Contrato del proveedor de tokens y envío de Authorization cuando corresponde, evitando `Bearer undefined`. |
| `design-software-web-shell/src/main.tsx` | Conexión del proveedor de tokens con `sessionStorage`. |

El formulario no guarda la contraseña. No se agregó un usuario administrador por fuera de IAM ni se sustituyó el login por una sesión simulada. Otras pantallas del dominio IAM podían seguir requiriendo su propio microfrontend.

### 3.3 Permisos reales del usuario

La usuaria consultó `/api/v1/auth/me` desde el navegador y obtuvo HTTP `200`, con `REF_HIERARCHY_MANAGE` entre sus permisos. Sin embargo, los controles de creación de geografía seguían bloqueados.

Se encontró que `GeographyHierarchy` recibía `canManage={false}` fijo. La corrección conectó la capacidad visual con los permisos reales:

| Archivo | Corrección |
| --- | --- |
| `design-software-web-shell/src/app/ShellRoot.tsx` | Entrega las features del usuario al montaje de remotes. |
| `design-software-web-shell/src/shell/remote/RemoteRoutes.tsx` | Pasa esos permisos al MFE de Reference Data. |
| `design-software-reference-data-mfe/src/contracts/remote-props.ts` | Comprueba códigos de permiso simples y variantes con alcances reconocidos. |
| `design-software-reference-data-mfe/src/App.tsx` | Calcula `canManageHierarchy` y lo entrega a la pantalla en lugar del `false` fijo. |

Esta comprobación habilita o deshabilita controles del frontend. **La autorización definitiva permanece en el backend**, que valida token, permiso y alcance. No se asignaron permisos a ciegas ni se desactivó esa protección.

### 3.4 Renovación de sesión con refresh token

La aplicación dejaba de mostrar datos al expirar el access token; los registros seguían en PostgreSQL. Se incorporó el consumo del mecanismo de refresh que IAM ya ofrecía, sin cambiar la duración de los tokens ni modificar IAM.

| Archivo | Cambio |
| --- | --- |
| `design-software-frontend-platform/src/auth-client/AuthClient.ts` | Conserva el refresh token recibido en login; llama a `/api/v1/auth/refresh`, guarda los dos tokens rotados, coordina una única renovación concurrente y limpia la sesión si falla. |
| `design-software-frontend-platform/src/http-client/HttpClient.ts` | Amplía el proveedor con acceso y almacenamiento opcional del refresh token. |
| `design-software-web-shell/src/main.tsx` | Almacena `sena.access_token` y `sena.refresh_token`; atiende los eventos internos de renovación y sesión expirada; elimina ambos al limpiar la sesión. |
| `design-software-reference-data-mfe/src/adapter/out/http/referenceDataHttp.ts` | Ante `401`, solicita renovación al shell y reintenta una sola vez con el token actualizado. Si vuelve a ser rechazado, notifica expiración. |

La renovación es reactiva ante una respuesta no autorizada; no significa que se haya extendido la duración del JWT. Después de incorporar este cambio era necesario volver a iniciar sesión para disponer del refresh token almacenado.

**Verificación limitada:** el código y las pruebas de rotación y reintento están presentes antes de R6. El seguimiento deja pendiente observar una expiración real de quince minutos y la rotación completa contra IAM. La usuaria confirma la incorporación de refresh, pero no se dispone en esta reconstrucción de una traza sanitizada de ese ciclo real; esa evidencia queda pendiente de verificar.

### 3.5 Listados de microrregiones, departamentos y municipios

Después de volver a iniciar sesión, ciertos registros no se veían aunque existían en PostgreSQL. Se identificó una causa distinta de la expiración: las consultas dependían de una selección padre que se reiniciaba al volver a entrar.

| Archivo | Cambio |
| --- | --- |
| `design-software-reference-data-mfe/src/App.tsx` | Coordina la pestaña activa y la carga de los niveles de geografía. |
| `design-software-reference-data-mfe/src/application/useMicroregions.ts` y `useDepartments.ts` | Permiten consulta sin filtro padre y conservan carga, paginación y filtros cuando hay selección. |
| `design-software-reference-data-mfe/src/application/useMunicipalities.ts` | Se reutiliza su modo existente `includeAll`; no se atribuye su creación a esta intervención. |
| `design-software-reference-data-mfe/src/adapter/out/http/microregionsService.ts` y `departmentsService.ts` | Omiten el filtro padre cuando no se seleccionó uno. |
| `design-software-reference-data-mfe/src/domain/geography.ts` | Ajusta los tipos de consulta para permitir ese uso. |
| `design-software-reference-data-mfe/src/adapter/in/components/GeographyHierarchy.tsx` | Permite listados sin selección padre y muestra el filtro aplicado; opción «Ver todos» para retirarlo. |

Se conservaron las relaciones obligatorias para crear hijos. Mostrar todos los registros accesibles no elimina las FK ni la autorización. La recuperación de todos los listados en una sesión real quedó solicitada en el seguimiento; no hay una evidencia individual completa para cada pestaña anterior a R6.

## 4. Backend, PostgreSQL y SQL inicial: qué se reutilizó

La integración conectó servicios y tablas existentes. Según el seguimiento y la conversación, no requirió cambiar la lógica de Go, el servicio Java de IAM ni el esquema para resolver proxies, sesión, permisos visuales y listados.

| Elemento | Clasificación por propósito |
| --- | --- |
| Changelogs iniciales de `design-software-reference-data-db` y `design-software-iam-db` | Preparación del esquema existente. Aplicarlos para instalar tablas o datos iniciales es parte del arranque funcional, no es R6 por el solo hecho de utilizar Liquibase. No se atribuye a esta intervención su autoría. |
| DML inicial y datos existentes de integración | Datos de arranque o para comprobar el dominio. La fecha exacta de ejecución de cada migración y la procedencia de cada fila quedan pendientes de verificar si no hay salida histórica. |
| Datos registrados desde la pantalla antes de R6 | Prueba funcional real de integración, confirmada por la usuaria y sus capturas en la conversación. |
| `Consultar_tablas_IAM_y_reference_data.sql` | Consultas de lectura para inspeccionar tablas existentes. No es una migración ni una modificación del esquema. |
| `Docs-reference-data/Ejecucion-base-de-datos.md` | Documentación de variables, carpetas y arranque de PostgreSQL, IAM, Reference Data y frontends. |

La conexión original de Reference Data utiliza `REFERENCE_DATA_DATABASE_URL`; IAM utiliza su configuración de conexión propia. Ambos acceden a la base local indicada, con separación por esquemas. No se incluyen contraseñas, claves privadas ni tokens en este informe.

## 5. Cómo se comprobó la integración

### Comprobaciones técnicas registradas antes de R6

El seguimiento registra resultados satisfactorios de:

- Salud del backend Reference Data y respuesta JSON a través del proxy del shell.
- Respuestas `401` sin token en rutas protegidas de IAM y Reference Data. Demuestran protección activa, no un login exitoso.
- Descarga de `remoteEntry.js` desde `5105`. Por sí sola no demuestra montaje del MFE; se complementa con la pantalla autenticada vista por la usuaria.
- Compilación del MFE, typecheck del shell y compilación federada del shell con `--mode development`.
- Prueba de navegador con Edge para mostrar el login público sin sesión y sin errores JavaScript.
- Pruebas de login, permisos visuales, listados, rotación concurrente de tokens y reintento ante `401`.

Las pruebas unitarias y de integración de frontend utilizaron HTTP simulado. **Eso no significa que el flujo funcional presentado a la usuaria fuera un mock.** Esas pruebas comprueban comportamientos específicos; la operación desde la pantalla y su consulta en PostgreSQL aportan evidencia distinta.

Archivos de pruebas identificados en el seguimiento:

- `design-software-web-shell/src/app/LoginPage.test.tsx`.
- `design-software-web-shell/e2e/login-public.spec.ts`.
- `design-software-web-shell/src/shell/remote/RemoteRoutes.test.tsx`.
- `design-software-frontend-platform/src/auth-client/AuthClient.test.ts`.
- `design-software-reference-data-mfe/tests/unit/sessionIntegration.test.ts`.
- `design-software-reference-data-mfe/tests/integration/AppData.test.tsx`.
- `design-software-reference-data-mfe/tests/integration/GeographyHierarchy.test.tsx`.

Estos resultados son históricos, no una nueva ejecución de pruebas realizada al escribir este informe.

### Confirmaciones reales de la usuaria anteriores a R6

En la conversación anterior a iniciar el reto se confirmó:

1. Inicio de sesión real y consulta de identidad con HTTP `200` y permisos reales.
2. Carga de Reference Data dentro del web-shell y presentación de formularios del dominio.
3. Registro de datos desde la pantalla; la usuaria confirmó que se guardaban en PostgreSQL, incluidos centros de formación.
4. Inspección de tablas con pgAdmin y permanencia de datos aun cuando la sesión expiraba.

Por tanto, existe confirmación funcional de un recorrido real pantalla → API → PostgreSQL. No se conserva aquí una traza completa con el mismo identificador de petición y fila para cada operación; no se afirma que se hayan certificado todas las HU ni todas las operaciones CRUD de las nueve tablas.

## 6. Cambios auxiliares anteriores a R6

El commit `5c45951` también incluye documentación por capas, comentarios de responsabilidades, retiro de `baseUrl` del `tsconfig.json` del shell y eliminación de una copia anidada duplicada del MFE. Son tareas de documentación, configuración y limpieza; no constituyen una nueva operación de negocio ni una migración de base de datos.

La incorporación del proyecto `design-software-design-system` en Git no demuestra que se haya desarrollado íntegramente durante esta integración. Se reutilizó como dependencia visual; no se le atribuye lógica de login, autorización ni persistencia.

## 7. Separación respecto del trabajo posterior

Se toma `5c45951` como límite del estado versionado previo a R6. Los commits y documentos anteriores permiten identificar la integración sin usar las pruebas posteriores del reto para completarla retrospectivamente.

En archivos que continuaron recibiendo modificaciones se consideran únicamente los bloques existentes en esa versión o respaldados por el seguimiento anterior. Por ejemplo, la modificación local posterior visible en `ShellRoot.tsx` es un comentario de responsabilidad; no se interpreta como una nueva corrección funcional de integración.

Quedan fuera de este informe las modificaciones de esquema, datos de prueba, pruebas de transición y reversiones cuyo propósito sea R6. Ningún resultado posterior de ese reto se utiliza aquí para afirmar una comprobación anterior.

## 8. Pendientes de verificar y conclusión del alcance

| Tema | Estado de la evidencia |
| --- | --- |
| Renovación real al expirar el access token | Implementación y pruebas simuladas documentadas; ciclo real completo pendiente de verificar. |
| Cada listado hijo después de reiniciar sesión | Corrección y pruebas documentadas; evidencia real individual por pestaña pendiente de verificar. |
| Ejecución histórica exacta de todas las migraciones iniciales | Se reutilizaron esquemas existentes; faltan salidas que permitan fechar cada aplicación. |
| Perfil Docker completo del dominio | La ejecución documentada es mixta. No se certifica aquí esa modalidad ni su equivalencia formal con el acta. |
| Autorización del instructor para ejecución local | Información aportada por la estudiante; soporte formal pendiente de verificar. |
| HU vigentes cerradas y PR del equipo fusionados en develop | No demostrado por esta integración local ni por los commits examinados. Requiere revisar el seguimiento del equipo y sus PR. |

**Resultado del trabajo previo:** se conectaron componentes existentes para permitir login real, uso de permisos, montaje de Reference Data en el shell y operaciones desde la pantalla sobre PostgreSQL. Se corrigieron problemas de transporte, sesión y visualización, sin presentar ese trabajo como cumplimiento del reto R6.
