ALTER TABLE institutional_structure.macroregion ADD COLUMN name_v2 TEXT; 

---  Lo que hace Liquibase
   -- Conserva la columna actual name y sus datos.
   -- Añade name_v2, donde prepararemos el nuevo almacenamiento.
   -- Permite temporalmente valores NULL: todavía no hemos copiado los nombres existentes.
   --El backend continúa leyendo y escribiendo en name.

   --Sin CASCADE: si más adelante existen objetos que dependan de esa columna, queremos que
   -- PostgreSQL nos avise, en lugar de eliminarlos automáticamente. Su retirada deberá planificarse 
   --explícitamente.