# design-software-iam-service (Java / Spring Boot)

authN/authZ/RBAC. Hexagonal **Maven multi-módulo** (los módulos enforzan el hexágono). ADR-006.

```
iam-core/       domain + application (PURO: sin Spring/JPA) + tests JUnit
iam-adapters/   in/rest · out/persistence (JPA -> iam_db) · out/security (BCrypt, JWT RS256)
iam-api/        Spring Boot app (bootstrap + wiring) + Dockerfile
contracts/      DTOs y API interfaces generados desde shared-contracts (openapi-generator)
```

authN se **federa** al IdP del SENA; iam conserva authZ/RBAC (ADR-005).

## Contrato

Los DTOs y las interfaces `*Api` **no se escriben a mano**: se generan en `contracts/` con
`openapi-generator` desde una copia fijada de `design-software-shared-contracts`
(`openapi/iam.yaml` + `openapi/_shared.yaml`). El commit del que provienen está en
[`contracts/openapi/PINNED-FROM.md`](contracts/openapi/PINNED-FROM.md); ahí también está el
procedimiento para actualizarlos. Nada generado se commitea.

## Configuración (toda por entorno, sin fallbacks de secretos)

| Variable | Obligatoria | Para qué |
|---|---|---|
| `IAM_DB_DSN` | sí | JDBC URL de iam_db, p. ej. `jdbc:postgresql://localhost:15432/design-software-develop` |
| `IAM_DB_USERNAME` | sí | Usuario de aplicación (miembro de `iam_writer`) |
| `IAM_DB_PASSWORD` | sí | Contraseña de ese usuario |
| `IAM_JWT_PRIVATE_KEY` | sí | Clave RSA privada PKCS#8 (PEM o base64) para firmar los access token RS256 |
| `IAM_JWT_ISSUER` | sí | Claim `iss`, exigido también al verificar |
| `IAM_JWT_KEY_ID` | no | `kid` del header (default `iam-rsa-1`), para rotación y JWKS |
| `IAM_JWT_PREVIOUS_PUBLIC_KEY` / `IAM_JWT_PREVIOUS_KEY_ID` | no | Clave pública RSA X.509 (PEM o base64) y `kid` de la clave saliente durante una ventana de rotación (HU-IAM-010 E3); ambas o ninguna. Se publican en `GET /api/v1/auth/.well-known/jwks.json` junto a la clave actual, así los tokens firmados antes de rotar siguen verificando localmente hasta que expiran (15 min). |
| `SPRING_RABBITMQ_HOST` | no | Broker RabbitMQ. **Si se define, se activa el relay de outbox** (HU-IAM-007) que publica en el exchange `iam-events`. Sin él, los eventos quedan solo *staged* en `identity.outbox`. |
| `SPRING_RABBITMQ_USERNAME` / `SPRING_RABBITMQ_PASSWORD` | condicional | Credenciales del broker cuando el relay está activo (sin defaults de secreto). |
| `SPRING_RABBITMQ_PORT` / `SPRING_RABBITMQ_VIRTUAL_HOST` | no | Puerto (`5672`) y vhost (`/`) del broker. |
| `IAM_EVENTS_EXCHANGE` | no | Nombre del topic exchange (default `iam-events`). |
| `IAM_OUTBOX_RELAY_POLL_INTERVAL_MS` / `IAM_OUTBOX_RELAY_BATCH_SIZE` | no | Cadencia (default `5000`) y tamaño de lote (default `100`) del poller. |

Si falta la clave o el issuer el servicio **no arranca**: es deliberado, no hay valor por defecto.

Generar una clave de desarrollo:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out iam-dev-key.pem
```

## Verificación local del JWT (HU-IAM-010)

El `access_token` (RS256) lleva `sub`, `roles`, `features` (pares `CODE:SCOPE_TYPE`
construidos con la evaluación de HU-IAM-006 — grants por rol más overrides, ver
[`AuthorizationPolicy.effectiveFeatureScopes`](iam-core/src/main/java/co/sena/iam/domain/service/AuthorizationPolicy.java)),
`training_center_id` y `exp`. El edge (Traefik, ADR-007) y cada servicio downstream verifican
firma y claims **sin llamar a iam** por request, usando la clave pública publicada en:

```
GET /api/v1/auth/.well-known/jwks.json
```

Formato JWKS estándar (RFC 7517); durante una rotación de clave se publican dos entradas
(actual + saliente, cada una con su `kid`). El detalle completo — estructura del JWT,
algoritmo de verificación por servicio, aplicación de scope en queries y rotación — está en
`07-api/authentication.md` en `design-software-docs` (fuente normativa).

## Build y pruebas

```bash
./mvnw test
```

Las pruebas unitarias (dominio, casos de uso, JWT y la capa REST) corren sin dependencias
externas. Las de integración terminan en `*IT`, corren en `verify` y necesitan un PostgreSQL
real con los esquemas de `design-software-iam-db`; sin las variables de entorno se saltan solas:

```bash
IAM_DB_DSN=jdbc:postgresql://localhost:15432/design-software-develop \
IAM_DB_USERNAME=design_software_app IAM_DB_PASSWORD=... \
./mvnw verify
```

`OutboxRelayPostgresIT` (HU-IAM-007) necesita además un broker RabbitMQ real; se salta solo si
falta `IAM_BROKER_HOST`:

```bash
IAM_DB_DSN=... IAM_DB_USERNAME=... IAM_DB_PASSWORD=... \
IAM_BROKER_HOST=localhost IAM_BROKER_USERNAME=app IAM_BROKER_PASSWORD=app \
./mvnw verify
```

## Eventos de dominio (outbox → `iam-events`)

Los casos de uso **stagean** eventos en `identity.outbox` dentro de la misma transacción que el
cambio de estado (`iam.user.created`, `iam.user.deactivated`, `iam.role.assigned` de HU-IAM-005;
`iam.session.started` en el login, HU-IAM-007). Un **relay** (`OutboxRelay`, poller `@Scheduled`
sobre `iam-api`) lee las filas no publicadas con `FOR UPDATE SKIP LOCKED`, las publica en el topic
exchange `iam-events` (routing key = `event_type`, message-id = `event_id`) como *envelope* de
`design-software-shared-contracts`, y marca `published_at`. Entrega **at-least-once**; los
consumidores deduplican por `event_id`. El relay solo se activa si hay broker configurado
(`SPRING_RABBITMQ_HOST`).

## Endpoints implementados

| Método | Ruta | HU | Notas |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | HU-IAM-001 | 200 tokens · 401 `IAM_INVALID_CREDENTIALS` · 423 `IAM_ACCOUNT_LOCKED` |
| `GET` | `/api/v1/auth/me` | HU-IAM-001 | Identidad + roles/features/módulos del portador |
| `GET` | `/health`, `/ready` | template | Sondas del contenedor, fuera de `/api/v1` |
| `GET` | `/api/v1/reports/login-audit` | HU-IAM-009 | Solo lectura · paginación por cursor · requiere feature `AUDIT_LOG_VIEW` · 403 `FORBIDDEN` sin el feature |

El resto de operaciones del contrato existe en la interfaz generada y responde `501` hasta que
su HU las implemente.

## Reporte de auditoría de login (HU-IAM-009)

`GET /api/v1/reports/login-audit` — reporte propio del dominio IAM sobre
`identity_audit.audit_login`, solo lectura (sin escritura desde este endpoint; el único
escritor sigue siendo `AuthenticateUser`, HU-IAM-001).

- **Paginación por cursor** (guidelines §6, colección grande/continua): orden
  `attempted_at DESC, id DESC` (más reciente primero, `id` como desempate estable). El cursor
  es opaco al cliente: base64 de un JSON `{"attempted_at","id"}` con la última fila de la
  página anterior. `limit` por defecto 50, máx 100 (`_shared.yaml#/components/parameters/limit`).
- **Filtros**: `user_id`, `email` (email intentado), `outcome`, `from`/`to` (rango inclusive/
  exclusive sobre `attempted_at`). Todos opcionales y combinables.
- **Autorización (E2, motor de HU-IAM-006)** — el feature requerido es `AUDIT_LOG_VIEW`
  (`x-required-feature` en el contrato). El controlador resuelve al solicitante con
  `GetCurrentUserUseCase` y evalúa `EvaluateAuthorizationUseCase` contra su centro de formación
  primario (`UserAccess.primaryTrainingCenterId()`): `SYSTEM_ADMIN` tiene el feature en scope
  `GLOBAL` (siempre pasa); `CENTER_DIRECTOR` lo tiene en scope `TRAINING_CENTER` (pasa si su rol
  cubre ese centro). Sin un grant que cubra el centro, `403 FORBIDDEN`. `audit_login` no tiene
  columna de centro de formación, así que el control es de acceso al endpoint completo, no un
  filtro fila a fila por centro.

## Reglas de negocio vivas

- **RN-IAM-01 (bloqueo progresivo)** — 5 fallos consecutivos bloquean 15 minutos; 10 bloquean
  24 horas; un login exitoso reinicia el contador y libera el bloqueo. La regla es pura y vive
  en `iam-core` (`LockoutPolicy`).
- **Duración de tokens** — access 15 min, refresh 7 días (`TokenPolicy`), como fija el contrato.
- El refresh token se entrega opaco al cliente y en `session.refresh_token` sólo se guarda su
  digest SHA-256.
- Todo intento de login (éxito o fallo) queda en `identity_audit.audit_login`.
