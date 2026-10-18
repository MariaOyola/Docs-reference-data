# Seguimiento E2E reference-data — 2026-10-18

## Corrección de botones de geografía

El usuario comprobó /auth/me desde su navegador: HTTP 200 y REF_HIERARCHY_MANAGE presente. Después informó que no podía crear en ninguno de los cuatro niveles. Se encontró en design-software-reference-data-mfe/src/App.tsx que GeographyHierarchy recibía canManage={false} fijo. Se sustituyó por canManage={canManageHierarchy}, calculado a partir de los permisos reales entregados por el shell. No se agregaron grants ni se modificó autorización backend.

Se añadieron dos casos en tests/integration/AppData.test.tsx para comprobar los cuatro botones y apertura de formularios con el permiso, y botones deshabilitados sin él. El transporte de estas pruebas es simulado. Para verificar en el navegador, recargar http://localhost:5000/reference-data con la sesión vigente y pulsar Nueva macrorregión. Los niveles hijos necesitan un padre válido para guardar. La escritura y la consulta en pgAdmin siguen pendientes hasta realizarlas.

Se trabajó sobre las carpetas principales disponibles, sin modificar las copias anidadas. No se encontró un ZIP ni un seguimiento anterior.

## Archivos modificados

| Archivo | Cambio y finalidad |
| --- | --- |
| design-software-web-shell/vite.config.ts | Proxy /api/v1/auth a IAM 8081 antes del proxy general. /reference-data-api a 8080 eliminando explícitamente ese prefijo. Conserva los proxies existentes; strictPort en 5000. |
| design-software-web-shell/.env.development | VITE_LOCAL_SESSION=false. Mantiene sesión HTTP y federación. |
| design-software-web-shell/src/app/ShellRoot.tsx | Entrega features de /auth/me a RemoteRoutes. |
| design-software-web-shell/src/shell/remote/RemoteRoutes.tsx | Pasa esos permisos al remote reference-data sin permisos fijos. |
| design-software-reference-data-mfe/.env.local | VITE_REFERENCE_DATA_API_BASE_URL=/reference-data-api. Archivo local posiblemente ignorado por Git; recrearlo al trasladar el proyecto. |
| design-software-reference-data-mfe/vite.config.ts | Proxy equivalente para uso independiente y strictPort en 5105. |
| design-software-reference-data-mfe/src/adapter/out/http/referenceDataHttp.ts | Base relativa al origen de la página, Headers y token actual de sessionStorage con la misma clave sena.access_token que AuthClient. |
| design-software-reference-data-mfe/src/contracts/remote-props.ts | Reconoce códigos y CODE:SCOPE válidos para habilitar controles; el backend sigue validando autorización y alcance. |
| design-software-reference-data-mfe/src/App.tsx | Utiliza esa comprobación y corrige la indicación errónea de añadir /api/v1. |
| design-software-reference-data-mfe/tests/unit/sessionIntegration.test.ts | Prueba token presente/ausente, Headers, URL relativa y permisos con alcance. Usa fetch simulado; no es evidencia E2E. |
| Seguimiento_E2E_reference-data.md | Este documento. |

El remote federado ejecuta su código bajo el origen del shell y lee allí sessionStorage, aunque descargue JavaScript de 5105. Una pestaña independiente en 5105 no comparte ese almacenamiento. No se implementó refresh automático: un token expirado produce 401.

## Comprobado

- Backend Reference Data /health: 200. IAM /api/v1/auth/me sin token: 401.
- Shell /reference-data: 200 HTML; no lo intercepta el proxy de API.
- Shell /reference-data-api/health: 200 JSON.
- Shell /api/v1/auth/me y /reference-data-api/macroregions sin token: 401.
- MFE http://localhost:5105/remoteEntry.js: 200 JavaScript. Esto todavía no demuestra montaje autenticado en React.
- MFE: npm run build pasó; dos archivos seleccionados, seis pruebas pasaron.
- Shell: npm run build pasó; dos archivos seleccionados, quince pruebas pasaron.
- Shell: npx vite build --mode development pasó con federación real. La compilación normal usa producción y activa stubs por su manifest vacío.
- React, react-dom y react-router-dom declaran singleton y versiones compatibles en ambos proyectos. El registro usa lazy, pero el runtime de federación también solicita remoteEntry de otros dominios al iniciar. Esas solicitudes a dominios apagados no impidieron mostrar el login en la prueba de navegador. No se requiere iniciar todos para mostrar el login; el montaje autenticado de reference-data sigue pendiente.

## Login real implementado (actualización)

El usuario autorizó un login real mínimo en el shell. /login ahora monta un formulario público fuera de RequireSession; no requiere iam-mfe para autenticarse. Otras pantallas de IAM siguen dependiendo de ese remote.

AuthClient envía email/password a POST /api/v1/auth/login mediante el proxy, valida el Bearer recibido, guarda access_token mediante el TokenProvider del shell y consulta /auth/me antes de confirmar el login. No reintenta automáticamente una contraseña rechazada. Limpia el token si /auth/me falla. No persiste contraseña ni refresh_token; no incorpora renovación automática. El token se mantiene en sessionStorage hasta logout/cierre de pestaña y tiene la expiración que determina IAM. Al recargar con un token rechazado por IAM se limpia y vuelve al login.

Archivos de esta actualización:

- design-software-frontend-platform/src/http-client/HttpClient.ts: setter opcional del proveedor de token y evita enviar Bearer undefined cuando no hay proveedor.
- design-software-frontend-platform/src/auth-client/AuthClient.ts: login, validación mediante /auth/me y limpieza del token rechazado.
- design-software-web-shell/src/main.tsx: setter de sena.access_token en sessionStorage.
- design-software-web-shell/src/app/data-source/session-data-source.ts y http-session-data-source.ts: puerto y adaptación de login real.
- design-software-web-shell/src/app/hooks/use-current-user.ts: sesión ausente cuando IAM rechaza y limpia el token.
- design-software-web-shell/src/app/ShellRoot.tsx: monta login público y devuelve a state.from, limitado a rutas internas; por defecto /reference-data.
- design-software-web-shell/src/app/LoginPage.tsx y login.css: formulario accesible, estado de envío y errores de IAM.
- design-software-web-shell/src/app/LoginPage.test.tsx: éxito, credenciales rechazadas, token rechazado por /me y retorno seguro. Transporte simulado sólo en pruebas.
- design-software-web-shell/e2e/login-public.spec.ts: prueba de navegador sin credenciales contra Vite real; puede usar Edge instalado mediante PLAYWRIGHT_CHANNEL=msedge.

Verificado: typecheck del shell; 22 pruebas seleccionadas del shell; 4 pruebas existentes de AuthClient; compilación federada con --mode development. Edge real mostró /login al visitar /reference-data sin errores JavaScript; captura en design-software-web-shell/test-results/login-public.png. El typecheck independiente de frontend-platform sigue pendiente: faltan sus dependencias locales (vitest/globals); sus fuentes sí se comprobaron como dependencia del shell. No se instalaron paquetes adicionales.

El usuario indicó que probará el login después. Quedan pendientes las credenciales reales, la sesión autenticada, el montaje autenticado del MFE, la escritura desde pantalla y su consulta en pgAdmin. No se afirma que esas pruebas hayan pasado.

Resultado final adicional: la prueba e2e/login-public.spec.ts pasó con Edge (1 prueba). Verificó redirección a /login, campos, botón habilitado, ausencia de token y ausencia de errores JavaScript. Las cuatro pruebas del login volvieron a pasar con las aserciones del body y Authorization; el typecheck del shell también pasó.

Para repetir pruebas del login desde la carpeta del shell:
```powershell
npm run typecheck
npm test -- src/app/LoginPage.test.tsx src/app/hooks/use-current-user.test.tsx src/app/data-source/resolve-session-data-source.test.ts src/shell/guards/RequireSession.test.tsx
$env:PLAYWRIGHT_CHANNEL = 'msedge'
npx playwright test e2e/login-public.spec.ts
Remove-Item Env:PLAYWRIGHT_CHANNEL
```

Abrir http://localhost:5000/login e introducir las credenciales reales allí. Vite aplica estos cambios de TS/CSS mediante HMR; si una pestaña conserva estado antiguo, recargarla. No hace falta recompilar los backends.

## Terminales PowerShell

Mantener los backends existentes. No recompilar Go ni Java: no se modificaron. Ejecutar estos comandos sólo si el proceso correspondiente está detenido y conservar sus variables actuales.

Terminal IAM:
```powershell
cd 'C:\Users\USUARIO\Downloads\matematicas\main\OneDrive\Desktop\reference-data\design-software-iam-service'
java -jar .\iam-api\target\iam-api-0.1.0.jar
```

Terminal Reference Data, conservar DATABASE_URL, IAM_JWT_ISSUER, IAM_JWKS_URL y PORT actuales:
```powershell
cd 'C:\Users\USUARIO\Downloads\matematicas\main\OneDrive\Desktop\reference-data\design-software-reference-data-service'
.\reference-data-api.exe
```

Terminal MFE (sirve el remote en 5105):
```powershell
cd 'C:\Users\USUARIO\Downloads\matematicas\main\OneDrive\Desktop\reference-data\design-software-reference-data-mfe'
# Sólo para recrear la configuración local si falta:
Set-Content .env.local 'VITE_REFERENCE_DATA_API_BASE_URL=/reference-data-api'
npm run dev -- --host localhost
```

Terminal shell (abrir http://localhost:5000):
```powershell
cd 'C:\Users\USUARIO\Downloads\matematicas\main\OneDrive\Desktop\reference-data\design-software-web-shell'
npm run dev -- --host localhost
```

Reiniciar el Vite correspondiente tras modificar .env o vite.config.ts. El comando dev usa .env.development. No usar --mode develop: no carga ese archivo. No activar VITE_LOCAL_SESSION para omitir login. Shell y MFE se iniciaron durante esta intervención; comprobar si siguen activos antes de abrir otras instancias.

Comprobaciones sin credenciales:
```powershell
Invoke-WebRequest http://localhost:5000/reference-data-api/health -UseBasicParsing
Invoke-WebRequest http://localhost:5105/remoteEntry.js -UseBasicParsing
```

En la carpeta del MFE:
```powershell
npm run build
npm test -- tests/unit/sessionIntegration.test.ts tests/unit/geographyServices.test.ts
```

En la carpeta del shell:
```powershell
npm run typecheck
npm test -- src/shell/remote/RemoteRoutes.test.tsx src/app/data-source/resolve-session-data-source.test.ts
npx vite build --mode development
```

## Pendiente: sesión y escritura reales

1. Resolver el login indicado arriba. Introducir credenciales privadamente en el navegador; no guardarlas en archivos ni variables VITE_*.
2. En Network comprobar POST /api/v1/auth/login mediante 5000 y respuesta access_token, token_type Bearer, expires_in. No copiar el token al informe.
3. Confirmar que el flujo guarda sena.access_token en sessionStorage del origen 5000. GET /api/v1/auth/me debe enviar Authorization y devolver identidad, features y modules reales.
4. Visitar /reference-data. Confirmar remoteEntry.js desde 5105 y GET /reference-data-api/macroregions con Bearer y respuesta 200. Revisar errores de React/federación y CORS. La respuesta 200 del archivo remoteEntry por sí sola no satisface esta comprobación.
5. Con REF_HIERARCHY_MANAGE y alcance válido, crear desde pantalla una macroregión con código único de hasta 10 caracteres. Confirmar POST exitoso y una nueva lectura con el mismo id. Ante 403 revisar los grants reales sin saltarse autorización.
6. En pgAdmin conectar a localhost:15432, base design-software-develop, y ejecutar esta consulta de sólo lectura sustituyendo el código por el creado:
```sql
SELECT id, name, code, is_active
FROM institutional_structure.macroregion
WHERE code = '<CODIGO_CREADO>';
```
Comparar id y valores con respuesta y pantalla. No se realizó esta escritura ni la comprobación en pgAdmin. No crear datos directamente por SQL como sustituto de la operación de pantalla.

7. Comprobar logout, limpieza del token y acceso tras expiración cuando esté resuelto el flujo real de login.
