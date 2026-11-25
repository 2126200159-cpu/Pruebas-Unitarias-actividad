# Carpeta de reportes de pruebas

Aquí quedan los entregables que generan las pruebas **cuando las ejecutas**:

| Archivo | Qué es |
|---|---|
| `reporte-pruebas.html` / `reporte-pruebas.md` | Resultado de todas las pruebas, agrupado por módulo |
| `documentos-prueba/VetPlus/logs/vetplus.log` | Log escrito durante las pruebas (fecha, hora, módulo, operación, tipo de excepción) |
| `documentos-prueba/VetPlus/imagenes/` | Carpeta de imágenes que usan las pruebas (se limpia sola) |
| `../target/surefire-reports/` | Reportes estándar de Maven |

## Cómo ejecutarlas

1. Abre el Panel de XAMPP y pulsa **Start** en **MySQL**.
2. En NetBeans: clic derecho al proyecto → **Test** (Alt+F6), o ejecuta `ejecutar-pruebas.bat`.

Las pruebas trabajan en la base **`vetplus_test`** (se crea sola). Nunca tocan la base real `vetplus`.

## `ejecucion-parcial/`

Resultado de las pruebas que **no necesitan base de datos** (validaciones, imágenes y logs: 32 pruebas), ejecutadas durante el desarrollo
sin contar con un servidor MySQL. No sustituyen al reporte completo: las pruebas de clientes, empleados, mascotas, servicios,
relaciones, transacciones y fotos en base de datos se ejecutan en tu equipo con MySQL encendido.
