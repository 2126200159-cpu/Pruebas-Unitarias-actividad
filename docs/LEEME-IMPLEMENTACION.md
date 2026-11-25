# VETPLUS · Conexión de las interfaces (implementación)

## Cómo arrancar

```
mvn clean javafx:run
```

* **Base de datos: MySQL/MariaDB de XAMPP.** Antes de abrir la aplicación, en el Panel de XAMPP pulsa **Start** en MySQL.
  La aplicación crea sola la base `vetplus`, las 17 tablas y los 6 servicios (usa `src/main/resources/db/schema.sql`).
  Datos por defecto de XAMPP: `localhost:3306`, usuario `root`, sin contraseña. Si tu MySQL usa otro puerto o contraseña,
  crea el archivo `C:\Users\<tu usuario>\.vetplus\conexion.properties` con:
  ```
  host=localhost
  puerto=3306
  base=vetplus
  usuario=root
  clave=
  ```
  Para revisar los datos abre phpMyAdmin (http://localhost/phpmyadmin) y entra a la base `vetplus`.
  `docs/vetplus.sql` es un script opcional para importar a mano en phpMyAdmin.
* Dependencia en el `pom.xml`: `org.mariadb.jdbc:mariadb-java-client` (más `slf4j-api`). `module-info.java` requiere `org.mariadb.jdbc`.
* **Primer acceso:** usuario `admin@vetplus.com` (también funciona escribir solo `admin`), contraseña `admin123`. Al entrar pide cambiarla (igual que cualquier
  empleado nuevo o restablecido, que recibe una contraseña temporal que se muestra una sola vez).

## Datos iniciales (ya vienen cargados)

Al abrir la aplicación se completan solos, sin borrar nada de lo que ya hayas capturado.

**Horario de la clínica:** lunes a viernes 9:00–19:00, sábado 9:00–15:00, domingo cerrado (referencia: estéticas y clínicas veterinarias de México abren de forma parecida, con sábado reducido). La agenda solo ofrece horas dentro del turno de cada profesional:
Matutino 9:00–14:00 · Vespertino 14:00–cierre (sábado 14:00–15:00) · Mixto todo el día.

**Tarifas de referencia (MXN):**

| Servicio | Precio | Duración | Referencia de mercado consultada |
|---|---|---|---|
| Consulta | $350 | 30 min | clínica privada entre $200 y $700 |
| Vacunación | $300 | 30 min | estimado (vacunas + desparasitación suman $500–$1,200 al año) |
| Desparasitación | $150 | 30 min | ~ $140 por dosis (esquema trimestral) |
| Cirugía | $2,800 | 90 min | esterilización en clínica privada, perro pequeño $2,500–$4,000 |
| Estética (baño y corte) | $350 | 60 min | $300–$550; promedio nacional ~$350 |
| Pensión | $300 por noche | — | $200–$460; lo más común $250–$350 |

Son **precios de referencia**, no de una clínica específica: ajústalos en *Servicios y tarifas*. Si una tarifa ya tenía un precio capturado, no se sobrescribe.

**Personal de ejemplo** (contraseña inicial `VetPlus2026`, pide cambiarla al entrar):

| Usuario | Rol | Turno |
|---|---|---|
| `lucia.torres@vetplus.com` | Médica · Medicina general | Matutino |
| `ricardo.mendoza@vetplus.com` | Médico · Cirugía y medicina interna | Vespertino |
| `fernanda.castillo@vetplus.com` | Estilista · Estética canina | Matutino |
| `diego.salazar@vetplus.com` | Estilista · Corte y estilizado | Vespertino |

Además hay 3 proveedores de ejemplo (teléfonos y correos ficticios) y 13 insumos con existencia, mínimo y precio de compra aproximados (vacunas, desparasitantes, material de curación y estética).
Todos estos nombres, teléfonos y correos son **ficticios**: reemplázalos con los datos reales de la clínica.

## Qué se hizo

| Capa | Dónde |
|---|---|
| Esquema + datos iniciales | `resources/db/schema.sql`, `db/Semilla.java` (datos), `docs/vetplus.sql` (todo en un script para phpMyAdmin) |
| Conexión, transacciones, hash PBKDF2 | `db/Db.java`, `db/Hash.java` |
| Repositorios (todas las consultas) | `repo/*Repo.java` |
| Sesión, navegación con dato (id), formatos, validaciones | `util/*` |
| Marcos (menú, barra, campana) | `ShellBase` + `Admin/Medico/EstilistaShellController` |
| Login real (sin la regla "med/est") | `LoginController` |
| 14 controladores de vista | `ctrl/*Controller.java` |
| Diálogos del capítulo 8 | `ctrl/Dialogos.java`, `util/FormDialog.java` |

Los FXML **no cambiaron** salvo agregar `fx:controller` en las 14 vistas (los marcos siguen sin él, como pide la guía).
Los `@FXML` de cada controlador se generaron leyendo los fx:id reales de cada FXML.

## Decisiones donde la guía dejaba hueco (cap. 8)

* **Alta/edición de propietario:** diálogo (la ficha no tiene campo de nombre). La ficha de la derecha queda de lectura.
* **Clic en el chip de estado** (Activo/Inactivo) de Clientes y de las tarjetas de Servicios: cambia el estado con confirmación.
* **Pensión:** al elegir Pensión en Agenda se pide la fecha de salida; se crea la estancia y se agregan *días × tarifa* a la cuenta de hoy.
* **Cuentas:** se crean/agrupan solas al finalizar atención, estética o pensión (una cuenta pendiente por propietario y día). `btnNuevaCuenta` abre un diálogo manual.
* **Descuento familiar:** 1 mascota 0 %, 2 → 15 %, 3 o más → 25 %, sobre mascotas distintas de la cuenta; el precio se copia al crear el renglón.
* **Cobro:** siempre el total; efectivo no puede ser menor; tarjeta/transferencia exigen referencia.
* **Compras:** recepción desde *Historial de compras* (selecciona orden Emitida → "Registrar recepción"), que suma existencias. Bajo mínimo = existencia **menor** al mínimo. Doble clic en un insumo lo edita; `Supr` quita un renglón de la orden.
* **Médica:** vacunas/desparasitaciones se registran solas al finalizar esos servicios (próxima: la revisión capturada, o +1 año / +3 meses). Si captura próxima revisión con hora, se programa la cita de Consulta.
* **Estilista:** el primer "Guardar avance" pasa el servicio a *En proceso*. Mis pacientes solo consulta tablas/columnas de estética.
* **Reportes:** los 5 botones cambian la tabla grande; exportar genera CSV (con BOM para Excel).
* **Campana:** menú con avisos simples por rol.
* **Diseño de los diálogos:** todos (formularios y avisos) usan `util/Estilo.java` + las reglas `.dialogo-vet` al final de `vetplus.css`: fondo crema, campos y combos con las clases de la app, botón principal naranja y botón secundario blanco, logo en la barra de título.
* **Validación de captura:** correo, dirección y CURP son obligatorios en propietarios y empleados (el correo también en proveedores). Teléfono: solo números (10 dígitos); al teclear una letra se rechaza y aparece un aviso rojo junto al campo. Nombres, apellidos, contacto y nombre de mascota: solo letras (acentos, ñ, espacios, guion, apóstrofo); al teclear un número se rechaza con aviso. La CURP se escribe en mayúsculas, solo letras y números, máximo 18. Todo se vuelve a validar al guardar con mensajes en el diálogo o en una alerta. Los empleados de ejemplo no traen dirección ni CURP: hay que capturarlas si se editan. El nombre de un insumo y el de una empresa sí admiten números.
* **Empleados:** el campo de acceso es solo **correo electrónico**, obligatorio y con formato válido (los usuarios anteriores sin @ se convierten solos a `@vetplus.com`). El botón **Eliminar empleado** borra de verdad si la persona no tiene citas ni órdenes de compra; si tiene historial ofrece pasarla a **Inactivo** para conservarlo. No se puede eliminar ni inactivar la propia cuenta con sesión abierta ni al único administrador activo.
* **Contraseña temporal visible:** la contraseña que administración asigna (alta o restablecimiento) se guarda en la columna `contrasena_temporal` y se puede ver en **Empleados → Ver contraseña temporal** (solo administración) **mientras el empleado no la haya cambiado**. En cuanto la cambia, esa columna se borra y la contraseña real, guardada con hash, no se puede ver ni recuperar: si la olvida se usa *Restablecer contraseña*. Requiere MariaDB (XAMPP): la columna se agrega sola a bases anteriores con `ADD COLUMN IF NOT EXISTS`.
* **Imágenes (empleados y mascotas):** se guardan como archivos PNG/JPG en `Documentos\VetPlus\imagenes` (la carpeta se crea sola si no existe) y en la base de datos solo queda el nombre del archivo (`foto_ruta`). Se valida el formato real (no la extensión), que la imagen se pueda leer y un máximo de 5 MB. Se escribe primero el archivo y después la base: si la base falla, el archivo nuevo se borra y la foto anterior queda intacta. Una imagen que ya no existe o no se puede leer se muestra como "sin foto" sin romper la pantalla. Al eliminar a la persona o la mascota también se borra su archivo.
* **Copiar texto:** en cualquier ventana de aviso, `Ctrl+C` o clic derecho → *Copiar texto*. Las contraseñas temporales (alta y restablecimiento de empleados) salen en una ventana con caja seleccionable y botón **Copiar contraseña**.
* **Resumen de la sesión:** al cerrar sesión se limpia `Sesion`.

## Estado de verificación (importante)

En el entorno donde se escribió no había JavaFX ni Maven, así que **no se ejecutó la aplicación**. Sí se comprobó:

* Las capas sin JavaFX (`db`, `repo`, `util` puras) **compilan** con javac.
* La lógica SQL (choque de horarios, suma de cobros por renglón) se probó en una versión previa con SQLite. **La versión MariaDB no se pudo probar contra un servidor** (no había uno disponible): las diferencias son pocas (`CONCAT`, `GROUP_CONCAT`, `TIME_FORMAT`, `INSERT IGNORE`), pero pueden salir errores de sintaxis al probar.
* Todos los `@FXML` existen en su FXML y todos los ids de plantillas usados existen.

Al compilar con JavaFX pueden salir errores menores de API; avísenme el mensaje y se corrige.

## Pruebas manuales (cap. 10.3 y por pantalla)

1. **Login:** admin/admin123 → cambio obligatorio; contraseña mala → alerta; empleado Inactivo no entra; "Recordarme" guarda solo el usuario.
2. **Empleados:** alta de un médico y una estilista (anotar contraseña temporal), duplicar correo → error, intentar inactivar al único admin → error, restablecer contraseña.
3. **Servicios:** capturar tarifas; cambiar una tarifa y comprobar que una cuenta ya creada no cambia de precio.
4. **Clientes:** propietario con 3 mascotas; CURP de 17 caracteres y teléfono de 9 dígitos deben rechazarse; clic en mascota abre su expediente.
5. **Agenda:** cita en el pasado y traslape → bloqueadas; domingo no aparece; Pensión pide fecha de salida.
6. **Médica:** iniciar atención desde Mi agenda; guardar borrador, salir y volver (carga el borrador); finalizar → cita Finalizada, aparece en historial y en Caja.
7. **Estética:** guardar avance → *En proceso*; finalizar → a Caja; en *Mis pacientes* no debe verse nada clínico.
8. **Caja:** 1 mascota 0 %, 2 mascotas 15 %, 3 mascotas 25 %; cobrar; cierre y Reportes coinciden.
9. **Compras:** insumo bajo mínimo muestra el aviso; emitir orden no cambia existencias; registrar recepción sí y el aviso desaparece.
10. Abrir **cada pantalla con la base vacía**: sin datos falsos ni avisos por error. Cerrar sesión y entrar con otro rol: sin datos del anterior.


## Reglas de negocio nuevas

* **Administrador general único:** solo puede haber un usuario con rol Administración; no se puede crear otro, ascender a un empleado, cambiarle el rol, inactivarlo ni eliminarlo.
* **Sin duplicados:** clientes por CURP, correo o mismo nombre + teléfono; empleados por correo y CURP; mascotas por nombre + especie dentro del mismo cliente; servicios por nombre.
* **Servicios con CRUD completo:** *Servicios y tarifas* → **Nuevo servicio** (nombre, tipo, precio y tiempo estimado) y **Eliminar servicio**. Los 6 servicios base no se pueden renombrar ni eliminar (el sistema los usa por nombre); un servicio con citas o cuentas tampoco se elimina (se puede desactivar). Precio de 0 a $1,000,000 y duración de 5 a 480 minutos.
* **Eliminación conjunta:** *Clientes y mascotas* → **Eliminar cliente y mascotas** borra al cliente con todas sus mascotas, vacunas y fotos en una sola transacción. Si hay citas, cuentas o estancias no se borra nada y se ofrece pasarlo a Inactivo. En el expediente, **Eliminar mascota** sigue la misma regla (alternativa: Inactiva).
* **CURP:** además de 18 caracteres, se valida su estructura (4 letras, fecha AAMMDD válida, sexo H/M, entidad, 3 consonantes, homoclave y dígito).

## Errores y log

* **Log:** `Documentos\VetPlus\logs\vetplus.log` (se crea solo; rota a `vetplus.log.1` a los 2 MB). Una línea por evento: `fecha hora | NIVEL | módulo | operación | tipo de excepción | mensaje`. Las reglas del negocio salen como `AVISO` y los fallos técnicos como `ERROR`. Nunca se escriben contraseñas, hashes, correos completos, CURP ni teléfonos (se enmascaran) ni el rastro completo de la excepción.
* **Mensajes al usuario:** una regla del negocio muestra su mensaje tal cual; un fallo de base de datos o de disco muestra un mensaje general sin SQL ni detalles internos; un error inesperado dice que quedó registrado en el log. Un manejador global recoge también lo que no se capturó. La aplicación sigue abierta después de cualquier error recuperable.

## Pruebas JUnit 5

Están en `src/test/java/algo/vetplusapp/`, una carpeta por módulo (el reporte las agrupa igual):

| Módulo | Qué cubre |
|---|---|
| `clientes` | alta, consulta, actualización, eliminación conjunta con mascotas, historial, duplicados y obligatorios |
| `empleados` | alta con contraseña temporal, inicio de sesión, actualización, eliminación (y su foto), historial, duplicados, administrador único, cambio de contraseña |
| `mascotas` | alta, consulta, actualización, eliminación, asociación con su propietario, validaciones |
| `servicios` | alta, consulta, actualización de costo y tiempo, eliminación, servicios base, precio histórico en cuentas |
| `relaciones` | llaves foráneas, "no eliminar con dependientes", citas (pasado, domingo, turno, traslape), órdenes de compra |
| `validaciones` | obligatorios, teléfono, nombres, correo, CURP, fechas, costos y tiempos |
| `transacciones` | commit, rollback por excepción y por error SQL, anidadas, operación de negocio atómica, recuperación |
| `imagenes` | guardar y recuperar, carpeta en Documentos, formatos incorrectos, inexistentes, errores de acceso y de disco, integridad ante fallos de base de datos |
| `logs` | campos del log, mensajes al usuario, sin datos sensibles, la aplicación continúa tras errores |

**Cómo ejecutarlas:** enciende MySQL en XAMPP y, en NetBeans, clic derecho al proyecto → **Test** (o `ejecutar-pruebas.bat`). Usan la base `vetplus_test`, nunca la real. Al terminar quedan en `reportes/` el reporte (`reporte-pruebas.html` y `.md`) y el log generado (`reportes/documentos-prueba/VetPlus/logs/vetplus.log`).

**Limitaciones:** las pruebas verifican la lógica (repositorios, validaciones, imágenes, log, transacciones); no automatizan clics en las pantallas JavaFX. Las 32 pruebas que no usan base de datos se ejecutaron durante el desarrollo (`reportes/ejecucion-parcial`); las demás requieren MySQL y se ejecutan en tu equipo.
