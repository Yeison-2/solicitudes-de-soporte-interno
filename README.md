# Solicitudes de soporte interno

Aplicación web para registrar, priorizar y atender solicitudes de soporte interno de una empresa con varios sitios de distribución. Las vistas se generan en el servidor con Thymeleaf y los datos se guardan en PostgreSQL.

## Alcance de esta versión (S1)

| Rol | Qué puede hacer en esta versión |
|---|---|
| Solicitante | Registrar solicitudes y consultar las propias con su estado |
| Coordinador | Ver todas las solicitudes y cambiar su prioridad |
| Agente | Ingresar; sus funciones llegan en la siguiente versión |
| Auditor | Ingresar; sus funciones llegan en una versión posterior |

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

## 4. Ejecutar las pruebas

```powershell
.\mvnw.cmd test
```

La suite cubre los criterios de aceptación de las cuatro historias de la versión S1: ingreso y roles, registro de solicitudes, consulta de las propias y priorización con registro del cambio.

## 5. Estructura del código

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

## 6. Seguridad

- La autorización se valida en el servidor por rol; ocultar enlaces en la pantalla es solo presentación.
- Las contraseñas se guardan cifradas con BCrypt.
- Los formularios incluyen protección CSRF.
- Las credenciales de base de datos se leen de variables de entorno.
