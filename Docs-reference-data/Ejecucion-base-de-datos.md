# Levantar el entorno develop

## 1. Verificar los changelogs

Párate en el repo `design-software-docker-infra-develop` y ejecuta:

```powershell
Test-Path ../design-software-iam-db/changelog/changelog-master.yaml
Test-Path ../design-software-reference-data-db/changelog/changelog-master.yaml
```

## 2. Levantar PostgreSQL en develop

Docker Desktop debe estar abierto.

```bash
docker compose --env-file .env.develop up -d postgres
```

Comprobar:

```bash
docker compose --env-file .env.develop ps postgres
```

Debe aparecer `healthy`, con el puerto `15432->5432`.

## 3. Crear o actualizar las tablas de reference-data

Desde esa misma carpeta:

```bash
docker compose --env-file .env.develop --profile tooling run --rm liquibase-reference update
```

## 4. Crear o actualizar las tablas de IAM

```bash
docker compose --env-file .env.develop --profile tooling run --rm liquibase-iam update
```

Hasta aquí PostgreSQL está funcionando y las migraciones de ambos dominios se aplicaron.

## 5. Entrar al repositorio del servicio IAM

```bash

```

## 6. Configurar su conexión a PostgreSQL

| Campo | Valor |
|---|---|
| Host | localhost |
| Puerto | 15432 |
| Base de datos | design-software-develop |
| Usuario | design_software_user |
| Contraseña | La de tu `.env.develop` |