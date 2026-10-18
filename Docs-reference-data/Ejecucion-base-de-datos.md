# Ejecución local: BD → backend → frontend

**Todos los comandos van en la TERMINAL POWERSHELL, no dentro de un archivo.** El `cd` te coloca en la carpeta correcta. Solo el SELECT final va en Query Tool de pgAdmin.

Abre Docker Desktop. Usa las terminales indicadas; no vuelvas a iniciar un servicio que ya está funcionando. Esta guía usa la configuración local ya preparada y solo el ambiente develop.

| Parte | Terminal | Carpeta dentro de reference-data | Archivo utilizado | Puerto |
| --- | --- | --- | --- | --- |
| Base de datos | 1 | design-software-docker-infra-develop | docker-compose.yml + .env.develop | 15432 |
| Backend IAM | 2 | design-software-iam-service | iam-api/target/iam-api-0.1.0.jar | 8081 |
| Backend reference-data | 3 | design-software-reference-data-service | reference-data-api.exe | 8080 |
| Frontend del dominio | 4 | design-software-reference-data-mfe | package.json, script dev | 5105 |
| Frontend principal | 5 | design-software-web-shell | package.json, script dev | 5000 |

## 1. BASE DE DATOS — terminal 1

**Para qué:** iniciar PostgreSQL.

```powershell
cd "C:\Users\USUARIO\Downloads\matematicas\main\OneDrive\Desktop\reference-data\design-software-docker-infra-develop"
docker compose --env-file .env.develop up -d postgres
docker compose --env-file .env.develop ps postgres
```

Esperado: `healthy`. Una base, `design-software-develop`, contiene los esquemas de IAM y reference-data.

**Solo en instalación inicial o si hay migraciones nuevas**, en esta misma carpeta:

```powershell
docker compose --env-file .env.develop --profile tooling run --rm liquibase-reference update
docker compose --env-file .env.develop --profile tooling run --rm liquibase-iam update
```

## 2. BACKEND IAM — terminal 2

**Para qué:** iniciar autenticación. Reemplaza `TU_CLAVE_POSTGRES` por POSTGRES_PASSWORD de `.env.develop`.

```powershell
cd "C:\Users\USUARIO\Downloads\matematicas\main\OneDrive\Desktop\reference-data\design-software-iam-service"
$env:IAM_DB_DSN = "jdbc:postgresql://localhost:15432/design-software-develop"
$env:IAM_DB_USERNAME = "design_software_user"
$env:IAM_DB_PASSWORD = "TU_CLAVE_POSTGRES"
$env:IAM_JWT_PRIVATE_KEY = Get-Content .\iam-dev-key.pem -Raw
$env:IAM_JWT_ISSUER = "https://iam.it.sena.local"
$env:IAM_JWT_KEY_ID = "iam-rsa-1"
$env:SERVER_PORT = "8081"
java -jar .\iam-api\target\iam-api-0.1.0.jar
```

**Deja esta terminal abierta.** La clave PEM y el JAR ya se generaron.

## 3. BACKEND REFERENCE-DATA — terminal 3

**Para qué:** iniciar tu API. Cuando pregunte, escribe POSTGRES_PASSWORD de `.env.develop`.

```powershell
cd "C:\Users\USUARIO\Downloads\matematicas\main\OneDrive\Desktop\reference-data\design-software-reference-data-service"
$clavePg = Read-Host "Contraseña de PostgreSQL" -AsSecureString
$claveTexto = [System.Net.NetworkCredential]::new("", $clavePg).Password
$claveUrl = [uri]::EscapeDataString($claveTexto)
$env:REFERENCE_DATA_DATABASE_URL = "postgres://design_software_user:${claveUrl}@localhost:15432/design-software-develop?sslmode=disable"
Remove-Variable clavePg, claveTexto, claveUrl
$env:IAM_JWT_ISSUER = "https://iam.it.sena.local"
$env:IAM_JWKS_URL = "http://localhost:8081/api/v1/auth/.well-known/jwks.json"
$env:PORT = "8080"
.\reference-data-api.exe
```

Esperado: `api listening on :8080`. **Deja esta terminal abierta.** El EXE ya se generó.

## 4. FRONTEND REFERENCE-DATA — terminal 4

**Para qué:** iniciar el microfrontend.

```powershell
cd "C:\Users\USUARIO\Downloads\matematicas\main\OneDrive\Desktop\reference-data\design-software-reference-data-mfe"
npm run dev
```

Puerto configurado: 5105. **Deja esta terminal abierta.** `npm install` ya se hizo; repetirlo solo si faltan o cambian dependencias.

## 5. FRONTEND WEB-SHELL — terminal 5

**Para qué:** iniciar la aplicación que contiene el login y carga el microfrontend.

```powershell
cd "C:\Users\USUARIO\Downloads\matematicas\main\OneDrive\Desktop\reference-data\design-software-web-shell"
npm run dev
```

**Deja esta terminal abierta.** Abre en el navegador:

1. http://localhost:5000/login
2. Ingresa con `admin@sena.edu.co` y su contraseña de aplicación, NO la de PostgreSQL.
3. Abre http://localhost:5000/reference-data/

Los ajustes de integración hechos por la otra IA deben conservarse. Esta guía no los sustituye.

## 6. COMPROBAR — terminal libre y pgAdmin

**En cualquier terminal PowerShell libre:**

```powershell
curl.exe -i http://localhost:8081/health
curl.exe -i http://localhost:8080/health/ready
```

Esperado: HTTP 200 en ambos.

**En pgAdmin → design-software-develop → Query Tool:**

```sql
SELECT * FROM institutional_structure.macroregion;
```

Para cerrar E2E falta crear un registro desde la pantalla, comprobarlo aquí y revisar la petición real. La restricción de creación sigue pendiente de resolver; no se ha confirmado una escritura completa desde el frontend.

## Solo si cambias código backend: recompilar

Detén el backend correspondiente con Ctrl+C antes de reemplazar su ejecutable. Luego recompila desde su carpeta y vuelve al paso de arranque.

**IAM — PowerShell dentro de design-software-iam-service:**

```powershell
docker run --rm --mount "type=bind,source=$($PWD.Path),target=/workspace" --mount "type=volume,source=ds-maven-cache,target=/root/.m2" -w /workspace maven:3.9-eclipse-temurin-21 mvn -DskipTests install
```

**Reference-data — PowerShell dentro de design-software-reference-data-service:**

```powershell
docker run --rm --mount "type=bind,source=$($PWD.Path),target=/workspace" --mount "type=volume,source=ds-go-mod-cache,target=/go/pkg/mod" --mount "type=volume,source=ds-go-build-cache,target=/root/.cache/go-build" -w /workspace -e GOOS=windows -e GOARCH=amd64 -e CGO_ENABLED=0 golang:1.25 go build -o reference-data-api.exe ./cmd/reference-data-api
```

**Recordatorios:** las variables se configuran en cada terminal nueva. Si aparece “port already in use”, comprueba la instancia existente; no abras otra ni cambies el puerto a ciegas. No guardes claves/tokens en esta guía ni elimines volúmenes.
