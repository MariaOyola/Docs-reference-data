# Prompt para redactar la descripción del Pull Request

Actúa como revisor técnico y redacta, en español y en primera persona, la descripción de mi Pull Request. Soy María José Rodríguez Oyola y mi reto individual es **13 — R6: evolución de esquema sin caída**, sobre `design-software-reference-data-db`.

Necesito explicar a Arquitectura qué hice realmente, en qué archivos, qué dependencias utilicé y qué resultados tienen evidencia. No necesito recomendaciones, un plan nuevo ni cambios de código: únicamente una descripción de PR lista para pegar en GitHub.

## Fuentes que debes revisar

Repositorio documental indicado por mí:

- https://github.com/MariaOyola/Docs-reference

Repositorio donde reuní el código y las evidencias locales:

- https://github.com/MariaOyola/Reference-data-evidencia

Repositorio de implementación de R6 y rama publicada:

- https://github.com/code-sena/design-software-reference-data-db/tree/hu-r6-dev

Dentro del workspace y de las fuentes disponibles, busca estos documentos por su nombre real:

- `Docs-reference-data/Cambios locales de integración E2E de reference-data previos a R6.md`.
- `Docs-reference-data/Informe del plan de mejora - Integracion y R6.md`.
- `Docs-reference-data/adr/ADR-014-macroregion-name-expand-contract.md`.
- `Docs-reference-data/R6 - Implementation overview for architects.md`.
- `Docs-reference-data/Ejecucion-base-de-datos.md`.
- `Docs-reference-data/Imagen.md` y sus imágenes relacionadas.
- `Seguimiento_E2E_reference-data.md`, si está disponible.

También revisa el texto adjunto titulado **“Informe unificado de integración local de reference-data”**. Es una reconstrucción de una etapa anterior, no una inspección nueva del código ni una afirmación vigente sobre todos los repositorios remotos. Si contradice el estado actual, contrasta fechas, commits y evidencias; no copies sus afirmaciones sin verificar.

No asumas que estos documentos están en la raíz de GitHub ni construyas enlaces blob sin comprobar su ubicación y rama. Si una fuente no es accesible, indícalo brevemente y utiliza únicamente lo que sí puedas revisar. No afirmes haber inspeccionado fuentes inaccesibles.

## Repositorios y carpetas que debes inspeccionar

```text
design-software-design-system
design-software-docker-infra-develop
design-software-frontend-platform
design-software-iam-db
design-software-iam-service
design-software-reference-data-db
design-software-reference-data-db-prueba
design-software-reference-data-mfe
design-software-reference-data-service
design-software-web-shell
Docs-reference-data
```

Esta lista identifica los componentes disponibles: **no demuestra que yo haya modificado todos sus archivos ni todos esos repositorios**. Distingue dependencias reutilizadas, preparación local, cambios de integración y cambios funcionales de R6. Una carpeta incorporada a un repositorio de evidencias no equivale a un cambio publicado en el repositorio oficial del componente.

## Criterios de revisión

- Revisa código, historial Git, diffs, documentos y salidas disponibles antes de atribuir cambios. No inventes archivos, funciones, pruebas, resultados, enlaces ni números de PR.
- Separa la **integración E2E anterior a R6** de la **implementación específica de R6**. Las correcciones de login, refresh token, proxies, permisos visuales y listados no deben presentarse como migraciones R6.
- Si un archivo recibió cambios en ambas etapas, distingue los bloques y su finalidad.
- No atribuyas a mi trabajo la creación completa de componentes existentes ni la autoría de migraciones iniciales reutilizadas.
- Clasifica por finalidad: un SQL inicial o un dato de integración no es R6 únicamente por ser SQL.
- Diferencia archivos locales, commits locales, commits publicados, PR abiertos y PR fusionados. Verifica el estado por repositorio. No inventes PR ni confundas push con merge.
- Las pruebas unitarias con HTTP simulado no equivalen a operaciones reales desde la interfaz. Distingue ambas evidencias.
- Una vista previa `update-sql` o `rollback-sql` no demuestra ejecución. Una salida de cero cambios revertidos no demuestra un rollback efectivo nuevo.
- Cero diferencias después del backfill no prueba por sí solo que se hayan copiado filas: revisa el estado anterior y las filas afectadas.
- No declares disponibilidad sin caída basándote solo en pruebas antes y después. Si no hay mediciones durante la migración, expresa esa limitación brevemente.
- No ocultes incidencias relevantes. En particular, revisa el rollback de develop, los changesets ajenos afectados y su posterior reconstrucción; no afirmes recuperación de eventos históricos sin evidencia.
- Nunca incluyas contraseñas, hashes de credenciales, tokens, claves privadas ni valores sensibles de archivos de entorno.
- No ejecutes migraciones, rollbacks ni operaciones destructivas para redactar la descripción.

## Contexto técnico que debes contrastar con el código

R6 interviene `institutional_structure.macroregion`: utiliza `name_v2 TEXT` como columna temporal para evolucionar `name VARCHAR(100) NOT NULL` a `name TEXT NOT NULL`, conservando el nombre utilizado por el backend.

Changesets de referencia:

```text
970_r6_expand_macroregion_name
980_r6_sync_macroregion_name
990_r6_backfill_macroregion_name
1000_r6_contract_macroregion_name
```

Debes explicar expansión, trigger de sincronización, backfill, contract transaccional y rollbacks según los archivos reales. No confundas rollback de transacción de prueba con rollback de Liquibase.

La ejecución funcional registrada utiliza PostgreSQL en Docker, IAM y Reference Data en Windows, y MFE y shell con Vite. Las bases locales documentadas usan develop:15432, qa:15433, staging:15434 y main:15435. Main local no equivale a producción remota, y probar esos ambientes no equivale a promover ramas Git.

La carpeta `design-software-reference-data-db-prueba` fue un ensayo de aprendizaje; distingue sus resultados de la implementación publicada en `design-software-reference-data-db`.

## Formato obligatorio de la respuesta

Devuelve **únicamente la descripción final del PR en Markdown**, sin título adicional ni texto antes o después. Debe contener SOLO los siguientes tres apartados, con estos encabezados exactos. Puedes usar tablas y listas dentro de ellos, pero no añadir otros apartados principales.

### Encabezado requerido: `## 1. Repositorios con evidencia de cambios locales`

- Separa mediante etiquetas o agrupaciones los ajustes previos de integración E2E y los cambios específicos de R6.
- Para cada repositorio con evidencia de cambios, identifica archivos, modificación concreta, finalidad y evidencia que respalda la atribución.
- Indica si el cambio es solo local, está publicado en una rama o está incluido en un PR identificado. Si no se verificó su estado remoto, dilo.
- Identifica los componentes utilizados sin cambios cuando sea necesario explicar dependencias, sin presentarlos como repositorios modificados.
- Añade enlaces reales a commits, archivos o evidencias revisados. Evita enumerar todos los archivos de un proyecto solo porque está copiado completo.

### Encabezado requerido: `## 2. Trabajo realizado en R6`

Explica de manera concreta:

- Tabla, columnas y tipo de dato original, temporal y final.
- Archivos SQL, changelogs, changesets y rollbacks involucrados.
- Qué implementé en expansión, sincronización, backfill y contract, y por qué permite conservar el contrato del backend.
- Qué datos de prueba y resultados se comprobaron realmente.
- Qué operaciones del servicio se probaron y en qué estado del esquema.
- Qué rollback se ejecutó en cada ambiente y qué datos u objetos se conservaron.
- Qué ajustes externos fueron necesarios para la ejecución y cuáles pertenecían a la integración previa, no a R6. No atribuyas cambios nuevos de backend, frontend o IAM sin diff que lo respalde.
- Las limitaciones de evidencia deben indicarse brevemente, sin presentar requisitos no demostrados como cumplidos.

### Encabezado requerido: `## 3. Enlaces y anexos`

Incluye únicamente enlaces o referencias a:

- ADR de mi decisión R6.
- Informe técnico y evidencias de pruebas: capturas, salidas o archivos de validación realmente disponibles.
- Cambios, commits o PR de repositorios relacionados de los que dependió la ejecución o integración.

Usa enlaces reales de GitHub verificados o rutas relativas válidas desde la ubicación del documento. En la descripción para el PR de base de datos, los documentos que viven en otro repositorio requieren enlaces a ese repositorio, no rutas relativas que salgan de él.

Si falta el enlace de un anexo o PR, escribe exactamente **“Pendiente de adjuntar”**. Los enlaces raíz aportados arriba son puntos de partida, no sustituyen comprobar la ruta del ADR, del informe o de cada evidencia.

No agregues recomendaciones, instrucciones para ejecutar comandos, conclusiones adicionales ni menciones a entregables ajenos a esos tres apartados.
