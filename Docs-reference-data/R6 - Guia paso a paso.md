# R6: evolución de esquema sin caída — guía paso a paso

Guía de aprendizaje para María José. Explica lo estudiado hasta ahora; no significa que las migraciones ya estén implementadas o probadas.

## 1. Qué debes lograr

Modificar una tabla real de Reference Data sin romper el servicio durante la transición y demostrar que puedes revertir el cambio.

El reto pide:

1. Migración **expand/contract** con Liquibase.
2. Datos de prueba y **backfill**, comprobando que se conservan correctamente.
3. Compatibilidad del servicio con el esquema anterior y el nuevo.
4. Rollback probado en Docker para `develop`, `qa`, `staging` y `main`.

Entregables: PR propio, ADR que justifique el diseño, informe breve con capturas y métricas, y sustentación individual de diez minutos con una modificación en vivo.

**Límite del trabajo:** implementar en `design-software-reference-data-db`. No modificar backend, frontend ni la base de IAM. Preparar los ambientes Docker de prueba es una actividad distinta de cambiar el esquema del dominio.

## 2. Lo que ya hiciste: ramas

Creaste y publicaste esta secuencia:

```text
main
 └─ qa
     └─ develop
         └─ hu-01-dev
```

Tu salida de `git branch -vv` confirmó que las cuatro ramas parten del commit `5c45951` y que `hu-01-dev` sigue a `origin/hu-01-dev`.

La implementación del reto irá en `hu-01-dev`.

**Una rama no crea un ambiente Docker ni una base de datos.** Antes de probar migraciones y rollback se prepararán ambientes aislados para no afectar la instalación que ya funciona.

## 3. Cambio propuesto para aprender el recorrido

Tabla real: `institutional_structure.macroregion`.

Definición actual, en `design-software-reference-data-db/01_ddl/03_tables/001_create_tables_institutional_structure.sql`:

```sql
name VARCHAR(100) NOT NULL
```

- `name`: nombre de la macrorregión.
- `VARCHAR(100)`: permite hasta 100 caracteres.
- `NOT NULL`: exige que exista un valor; por sí solo no prohíbe una cadena vacía.

La propuesta es sustituir su almacenamiento por una columna `TEXT`, conservando al final el nombre `name` que utiliza el backend. Durante la transición habrá una columna temporal llamada `name_v2`.

```text
Antes:       name VARCHAR(100)
Transición:  name VARCHAR(100) + name_v2 TEXT
Después:     name TEXT
```

`TEXT` no declara el límite de 100 caracteres. Sin embargo, **el backend conserva sus validaciones actuales**: esta migración no amplía por sí sola los nombres que acepta la API.

Se revisó `design-software-reference-data-service/internal/adapter/out/postgres/hierarchy.go`: el backend utiliza explícitamente `id, name, code, is_active` para macrorregiones. Mantener esos nombres es una condición de compatibilidad, no una prueba suficiente por sí sola: habrá que demostrar lecturas y escrituras reales en cada fase.

## 4. Las fases, con palabras sencillas

| Fase | Qué significa | Qué prepararás |
| --- | --- | --- |
| **Expand** | Ampliar sin retirar lo anterior. | Añadir `name_v2` y el mecanismo de sincronización. El backend sigue utilizando `name`. |
| **Backfill** | Completar la estructura nueva con los datos existentes. | Copiar los nombres y verificar que no faltan ni difieren. |
| **Compatibilidad** | Demostrar que el servicio sigue funcionando durante el cambio. | Probar consultas, creaciones y modificaciones antes, durante y después. |
| **Contract** | Retirar lo anterior cuando sea seguro. | Tras validar, retirar la columna antigua y renombrar la nueva como `name`, conservando las restricciones necesarias. |
| **Rollback** | Deshacer una fase de manera controlada. | Restaurar el esquema correspondiente y comprobar datos y funcionamiento. |

No ejecutaremos expansión, copia y retirada a ciegas en un único paso. Habrá comprobaciones entre fases.

### La expansión

**La expansión agrega una estructura nueva conservando la que usa el sistema actual.** En nuestro ejemplo agregamos una columna, no un registro:

```text
Antes:    name
Después:  name + name_v2
```

El backend sigue leyendo y escribiendo en `name`. Todavía no utiliza `name_v2`. Antes de instalar el trigger, una nueva macrorregión se guarda en `name` y su `name_v2` queda en NULL.

Los comandos anteriores sirvieron para consultar el estado, instalar la estructura inicial, validar la migración y marcar un punto de regreso con Liquibase. **No todos son parte de la expansión:** varios fueron preparación y diagnóstico.

Después haremos, por separado:

- **Trigger:** sincronizar `name_v2` cuando se creen o modifiquen registros en `name`.
- **Backfill:** copiar los nombres que ya existían.
- **Contract:** retirar la estructura anterior cuando las comprobaciones demuestren que es seguro.

**El trigger sincroniza los valores; no reemplaza la columna.** Agregar `name_v2` es el primer paso de la expansión; falta preparar la sincronización.

En la prueba `r6-develop`, Liquibase reportó la ejecución del cambio `970_r6_expand_macroregion_name`. La comprobación de las columnas sigue pendiente de compartir su resultado.

## 5. Por qué necesitamos backfill y trigger

Son dos mecanismos distintos:

| Mecanismo | Qué resuelve |
| --- | --- |
| **Backfill** | Copia los datos que ya estaban guardados. |
| **Trigger** | Mantiene sincronizadas las escrituras mientras conviven ambas columnas. |

Un trigger es una función que PostgreSQL ejecuta automáticamente cuando ocurre un evento configurado en una tabla. En este diseño debe mantener `name_v2` igual a `name` al crear o modificar un registro.

### Ejemplo sin trigger

1. El backfill copia los registros existentes de `name` a `name_v2`.
2. Después alguien crea **Región Caribe** desde la pantalla.
3. El backend escribe en `name`, como siempre.
4. Sin sincronización, `name_v2` podría quedar vacío para ese registro.
5. Al retirar `name`, podríamos perder el nombre o encontrar un error por las restricciones de la estructura nueva.

Por tanto, **no es que necesariamente falle el registro original**: puede guardarse correctamente en la columna antigua y faltar en la nueva.

### Qué debe hacer el trigger

```text
INSERT: se crea una macrorregión
        → copiar name a name_v2

UPDATE: se modifica su nombre
        → actualizar también name_v2
```

El trigger se instalará **antes del backfill**. La sincronización formará parte de la misma operación de escritura: no será una tarea posterior del frontend.

El diseño explicado sincroniza de `name` hacia `name_v2`, porque el backend existente sigue escribiendo en `name`. No hemos definido un segundo cliente que escriba directamente en `name_v2`.

Además del trigger, el backfill deberá diseñarse para no sobrescribir una modificación concurrente con un valor antiguo. La estrategia SQL concreta y sus pruebas todavía están pendientes.

## 6. Cuándo sería seguro hacer contract

No basta con que exista `name_v2`. Antes de retirar la columna anterior habrá que demostrar:

- Que todos los nombres se copiaron correctamente.
- Que las nuevas creaciones y modificaciones permanecen sincronizadas.
- Que se preservan las restricciones requeridas, como `NOT NULL`.
- Que ningún objeto dependiente impide el cambio o queda roto.
- Que el backend seguirá encontrando `name` al finalizar la transacción.
- Que existe un rollback definido y probado.

La retirada y el cambio de nombre se prepararán dentro de una transacción breve. El orden exacto de eliminación del trigger, restricciones y columnas se definirá antes de ejecutarlo.

**Sin caída no significa sin bloqueos.** PostgreSQL necesita bloqueos para ciertos cambios estructurales. Tendremos que limitar la espera y medir si las consultas y escrituras siguen respondiendo durante la prueba.

## 7. Qué significa rollback en este ejemplo

Rollback no es borrar la base ni eliminar los registros creados por los usuarios. Es revertir el cambio de esquema de una fase conservando los datos que deben permanecer.

Volver de `TEXT` a `VARCHAR(100)` requiere comprobar primero que los nombres caben. Si existe un nombre más largo, **no debe truncarse silenciosamente**: la reversión debe detenerse de manera controlada o aplicar una estrategia previamente acordada.

Cada fase necesitará su rollback. Las pruebas en `develop`, `qa`, `staging` y `main` se harán sobre entornos Docker aislados preparados para el reto; no sobre una base real de producción.

## 8. Cómo interviene Liquibase

Liquibase registra las migraciones mediante **changesets**: unidades identificadas de cambio, con su operación y su reversión cuando corresponda.

El punto de entrada actual es:

`design-software-reference-data-db/changelog/changelog-master.yaml`

Incluye los changelogs de DDL, DML, DCL y TCL del repositorio.

**No editaremos el SQL original ya aplicado para introducir esta evolución.** Añadiremos nuevos changesets y sus rollbacks, respetando el historial. La ubicación y los identificadores concretos se explicarán al preparar los archivos.

## 9. Punto actual del trabajo

| Actividad | Estado |
| --- | --- |
| Crear y publicar ramas | Hecho por María José. |
| Entender la propuesta sobre `macroregion.name` | Explicado; diseño detallado pendiente. |
| Entender backfill y trigger | Explicado con ejemplos. |
| Preparar ambientes Docker aislados | Pendiente. |
| Escribir y ejecutar changesets | Pendiente. |
| Probar compatibilidad, backfill y rollback | Pendiente. |
| Elaborar ADR, informe y PR | Pendiente. |

No se han ejecutado migraciones del R6 por crear este documento.

### Siguiente comprobación: conocer los datos actuales

En pgAdmin, conectar a `design-software-develop` y abrir Query Tool. Ejecutar solamente esta consulta de lectura:

```sql
SELECT id, name, code
FROM institutional_structure.macroregion
ORDER BY code;
```

Sirve para reconocer los registros que deben conservarse. No modifica tablas ni datos. Después se preparará la expansión paso a paso, antes de aplicarla en un ambiente de prueba.

## 10. Explicación corta para recordar

> Primero añado la estructura nueva sin retirar la anterior. Instalo un trigger para sincronizar las nuevas escrituras y hago backfill de los datos existentes. Compruebo que ambas estructuras coinciden y que el servicio sigue consultando y guardando. Sólo entonces retiro la estructura anterior de forma controlada, conservando el contrato del backend. Preparo y pruebo cómo revertir cada fase sin perder datos.
