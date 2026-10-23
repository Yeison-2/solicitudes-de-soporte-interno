# Solicitudes de soporte interno

Aplicación web para registrar, priorizar y atender solicitudes de soporte interno de una empresa con varios sitios de distribución. Las vistas se generan en el servidor con Thymeleaf y los datos se guardan en PostgreSQL.

## Alcance de esta versión (S3)

| Rol | Qué puede hacer en esta versión |
|---|---|
| Solicitante | Registrar solicitudes con prioridad, consultar las propias, buscar entre ellas, confirmar la solución o reabrirla con un motivo |
| Coordinador | Ver todas, cambiar la prioridad (justificación y fecha objetivo cuando es alta), asignar un agente activo, consultar indicadores agregados y exportar un reporte |
| Agente | Ver las solicitudes asignadas, recibir avisos, comentar avances y mover el estado por las transiciones permitidas |
| Auditor | Consultar el historial de cambios en modo lectura y abrir el detalle de una solicitud sin acciones |

### Rutas principales

| Ruta | Quién | Para qué |
|---|---|---|
| `/solicitudes` · `/solicitudes/nueva` | Solicitante | Listar las propias y registrar una nueva |
| `/solicitudes/{id}` | Dueño, coordinador, agente asignado, auditor | Detalle con comentarios, historial y acciones según el rol |
| `/solicitudes/mis-asignadas` · `/notificaciones` | Agente | Bandeja de trabajo y avisos de asignación |
| `/solicitudes/buscar` | Todos | Búsqueda por texto con filtros de estado, prioridad y categoría |
| `/coordinacion/solicitudes` | Coordinador | Bandeja con prioridad, filtro de estado y asignación |
| `/coordinacion/indicadores` | Coordinador | Volumen por estado y tiempo mediano de ciclo del conjunto filtrado |
| `/coordinacion/reporte.csv` | Coordinador | Descarga del reporte con los filtros aplicados |
| `/auditoria` | Auditor, coordinador | Historial de cambios, solo lectura |

## Requisitos del entorno

- Java 25 (con `JAVA_HOME` configurado).
- PostgreSQL en `localhost:5432`.
- Maven: se usa el wrapper incluido (`mvnw.cmd`).

## 1. Preparar la base de datos

Conéctese a PostgreSQL como administrador y ejecute:

```sql
CREATE USER user_software WITH PASSWORD '1234';
CREATE DATABASE solicitudesdb OWNER user_software;
GRANT ALL PRIVILEGES ON DATABASE solicitudesdb TO user_software;
```

Las tablas se crean solas al arrancar la aplicación.

Si su usuario o su contraseña son distintos, no modifique el archivo de configuración: defina las variables de entorno `DB_USERNAME` y `DB_PASSWORD` antes de arrancar.

## 2. Ejecutar la aplicación

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación queda en `http://localhost:8086`.

## 3. Ingresar

Al arrancar, la aplicación crea estas cuentas si no existen:

| Usuario | Contraseña | Rol |
|---|---|---|
| `coordinador1` | `Clave#2026` | Coordinador |
| `agente1`, `agente2` | `Clave#2026` | Agente |
| `solicitante1`, `solicitante2` | `Clave#2026` | Solicitante |
| `auditor1` | `Clave#2026` | Auditor |

Las claves se guardan cifradas. Son cuentas de prueba para trabajar en local; en un entorno compartido la clave se define con la variable de entorno `APP_SEED_PASSWORD` y no se usan estos valores.

## 4. Flujo de trabajo de una solicitud

1. El solicitante registra la solicitud; si elige prioridad alta, indica justificación y fecha objetivo.
2. El coordinador ajusta la prioridad y asigna un agente activo; el agente recibe un aviso.
3. El agente inicia la atención, registra comentarios y marca la solicitud como resuelta.
4. El solicitante confirma la solución (la solicitud queda cerrada) o la reabre indicando el motivo.

Cada paso queda registrado en el historial de la solicitud: fecha, autor, acción, campo y valores anterior y nuevo.

## 5. Consulta, indicadores y reporte

- La búsqueda aplica el texto sobre título y descripción y combina los filtros con AND; cada rol solo ve las solicitudes que le corresponden.
- Los indicadores muestran el volumen por estado (incluidos los estados en cero) y el tiempo mediano de ciclo de las solicitudes cerradas; no hay rankings ni métricas por persona.
- El reporte CSV incluye solo columnas estructuradas: `ID`, `Categoria`, `Prioridad`, `Estado`, `FechaCreacion`, `FechaActualizacion`, `AgenteAsignado`, `FechaCierre`. No incluye texto libre ni credenciales, y cada descarga queda registrada en el historial.

## 6. Ejecutar las pruebas

```powershell
.\mvnw.cmd test
```

La suite cubre los criterios de aceptación de las doce historias: ingreso y roles, registro y consulta de solicitudes, priorización, asignación con aviso, comentarios, transiciones de estado, cierre o reapertura, búsqueda con filtros por rol, indicadores agregados, historial de auditoría y reporte CSV.

## 7. Estructura del código

```text
src/main/java/com/jdc/web2026i/
  controller/   recepción de peticiones y vistas
  services/     reglas de negocio
  repository/   acceso a datos (Spring Data JPA)
  entities/     tablas de la base de datos
  DTO/          datos de los formularios
  Config/       seguridad y datos iniciales
src/main/resources/
  templates/    vistas Thymeleaf
  static/css/   hoja de estilos
  application.properties
src/test/java/com/jdc/web2026i/   pruebas
```

## 8. Seguridad

- La autorización se valida en el servidor por rol y por pertenencia a la solicitud; ocultar enlaces en la pantalla es solo presentación.
- Las contraseñas se guardan cifradas con BCrypt.
- Los formularios incluyen protección CSRF.
- El historial de auditoría es de solo lectura y no expone nombres completos ni credenciales.
- Las credenciales de base de datos se leen de variables de entorno.
