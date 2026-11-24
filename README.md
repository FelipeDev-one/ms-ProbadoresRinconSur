# Probadores Rincón Sur — Microservicios

Backend de la app **Probadores Rincón Sur** (DSY1105 · Sumativa 3 · EQ020).
Tres microservicios Spring Boot desacoplados, cada uno con su propia base de datos MySQL,
registrados en **Eureka** y expuestos a la app a través de un **API Gateway**.

```
                         ┌──────────────────────────┐
  App móvil (Retrofit) ─▶│ api-gateway        :8080 │── Swagger unificado: /swagger-ui.html
                         └────────────┬─────────────┘
                     lb:// (Eureka)   │
        ┌─────────────────────────────┼─────────────────────────────┐
        ▼                             ▼                             ▼
 probadores-service :8081      turnos-service :8082         usuarios-service :8083
 BD rinconsur_probadores       BD rinconsur_turnos          BD rinconsur_usuarios
        ▲                        │      │                           ▲
        └──── WebClient ─────────┘      └────────── WebClient ──────┘
              (resumen de probadores)        (¿el RUT es un cliente activo?)

                         eureka-server :8761 (registro y descubrimiento)
```

| Proyecto | Puerto | Responsabilidad | Base de datos |
|---|---|---|---|
| `eureka-server` | 8761 | Registro y descubrimiento de servicios | — |
| `api-gateway` | 8080 | Punto de entrada único y Swagger unificado | — |
| `probadores-service` | 8081 | Probadores de la sección y su estado | `rinconsur_probadores` |
| `turnos-service` | 8082 | Fila de espera y tiempo estimado | `rinconsur_turnos` |
| `usuarios-service` | 8083 | Cuentas de clientes y colaboradores, ingreso y contraseñas | `rinconsur_usuarios` |

Ningún servicio lee las tablas de otro: cuando turnos necesita datos de probadores o de usuarios,
los pide por HTTP con **WebClient** (`@LoadBalanced`, resuelto por Eureka con el nombre del servicio).

Tecnologías: Spring Boot 4.2 · Spring Cloud 2026.0 · JDK 21 · Maven · JPA/Hibernate · MySQL 8 ·
Lombok · Bean Validation · springdoc-openapi (Swagger) · JUnit 5 + Mockito · JaCoCo.

## Cómo levantarlo

1. **MySQL** en el puerto 3306 con usuario `root` sin contraseña (Laragon o XAMPP: botón *Start All*).
   Las bases de datos se crean solas. Si tu MySQL usa otra clave, define la variable `DB_PASSWORD`.
2. Ejecutar en este orden (en IntelliJ con la clase `*Application`, o en una terminal dentro de cada carpeta):

   ```bash
   ./mvnw spring-boot:run
   ```

   1. `eureka-server` → esperar a que abra http://localhost:8761
   2. `probadores-service`, `turnos-service` y `usuarios-service` (en cualquier orden)
   3. `api-gateway`
3. En el panel de Eureka (http://localhost:8761) deben aparecer los 4 clientes.
   El Gateway puede responder **503 durante unos 30 segundos** mientras se registran: es normal.

**Sin datos fijos:** las bases parten vacías. La primera vez, la app pide crear la cuenta de jefatura
(`POST /api/usuarios/configuracion-inicial`) y los probadores; desde ahí todo se ingresa desde la app.

## Documentación (Swagger / OpenAPI)

- **Unificada en el Gateway:** http://localhost:8080/swagger-ui.html (selector arriba a la derecha para cambiar de servicio).
- Por servicio: http://localhost:8081/swagger-ui.html · http://localhost:8082/swagger-ui.html · http://localhost:8083/swagger-ui.html

Cada endpoint indica sus respuestas posibles (200, 201, 400, 401, 404, 409, 503) y trae ejemplos para probarlo con *Try it out*.

## Endpoints (a través del Gateway: `http://localhost:8080`)

### usuarios-service

| Método | Ruta | Cuerpo | Respuestas |
|---|---|---|---|
| GET | `/api/usuarios/estado-inicial` | — | 200 `{"configurado"}` |
| POST | `/api/usuarios/configuracion-inicial` | `{"rut","nombre","contrasena"}` | 201 jefatura + código de recuperación · 409 ya configurado |
| POST | `/api/usuarios/registro` | `{"rut","nombre","contrasena"}` | 201 cliente + código de recuperación · 400 · 409 |
| POST | `/api/usuarios/colaboradores` | `{"rut","nombre","rol","turnoLaboral"}` | 201 (sin clave: la elige en su primer ingreso) · 400 · 409 |
| POST | `/api/usuarios/login` | `{"rut","contrasena"}` | 200 · 401 (indica intentos restantes) · 403 inactiva · 409 sin clave · 423 bloqueada 60 s |
| GET | `/api/usuarios?rol=&buscar=` | — | 200 lista (filtros opcionales) |
| GET / PUT | `/api/usuarios/{rut}` | `{"nombre","rol","turnoLaboral","activo"}` (todos opcionales) | 200 · 404 · 409 última jefatura activa |
| POST | `/api/usuarios/{rut}/contrasena` | `{"contrasena"}` | primera clave de un colaborador |
| PUT | `/api/usuarios/{rut}/contrasena` | `{"actual","nueva"}` | 204 · 401 clave actual incorrecta |
| POST | `/api/usuarios/{rut}/recuperar` | `{"codigo","nueva"}` | 200 nuevo código · 401 código incorrecto |
| DELETE | `/api/usuarios/{rut}/contrasena` | — | la jefatura restablece la clave de un colaborador |

Las contraseñas y los códigos de recuperación se guardan con **BCrypt** y nunca se devuelven. El RUT se valida con dígito verificador.

### turnos-service

| Método | Ruta | Cuerpo | Respuestas |
|---|---|---|---|
| GET | `/api/turnos?fecha=2026-11-24` | — | 200 todos los turnos o los de una fecha |
| GET | `/api/turnos/fila` | — | 200 turnos abiertos de hoy (preferentes primero) |
| POST | `/api/turnos` | `{"prendas":3,"preferente":false,"clienteRut":"19234587-1"}` | 201 · 400 prendas fuera de rango · 404 RUT no es cliente activo · 409 ya tiene turno · 503 |
| PUT | `/api/turnos/{id}` | turno completo (sincronización desde la app) | 200 · 400 · 404 · 409 número repetido o turno cerrado |
| GET | `/api/turnos/cliente/{rut}` | — | 200 turno abierto del cliente · 404 |
| GET | `/api/turnos/{numero}/espera` | — | 200 personas antes y minutos estimados · 404 · 503 |
| PUT | `/api/turnos/{id}/estado` | `{"estado":"ABANDONO","motivo":"ESPERA_LARGA"}` | 200 · 400 · 404 · 409 turno cerrado |
| GET / PUT | `/api/turnos/parametros` | máximo de prendas, tiempos, umbrales, horario | 200 · 400 fuera de rango |

### probadores-service

| Método | Ruta | Cuerpo | Respuestas |
|---|---|---|---|
| GET | `/api/probadores?estado=LIBRE` | — | 200 lista (filtro opcional) · 400 |
| GET | `/api/probadores/resumen` | — | 200 `{"total","habilitados","libres"}` |
| GET | `/api/probadores/{codigo}` | — | 200 · 404 |
| POST | `/api/probadores` | `{"tipo":"ACCESIBLE"}` | 201 con el primer código libre · 400 · 409 máximo 30 |
| PUT | `/api/probadores/{codigo}` | probador completo (sincronización) | 200 · 400 · 409 transición no permitida |
| PUT | `/api/probadores/{codigo}/estado` | `{"estado":"OCUPADO"}` | 200 · 400 · 404 · 409 transición no permitida |
| DELETE | `/api/probadores/{codigo}` | — | 204 · 409 con usos, en uso o último |
| GET / PUT | `/api/probadores/usos[/{id}]` | uso de probador | 200 · 400 devuelve más prendas de las que entraron · 404 |
| GET / POST | `/api/probadores/cambios` | historial de cambios de estado | 200 / 201 (no duplica) · 404 |

Errores: siempre `{"estado": 400, "mensaje": "...", "campos": {"prendas": "no puede ser más de 6"}}`
(el bloque `campos` aparece solo cuando falla una validación).

## Pruebas unitarias y cobertura (JaCoCo)

Las pruebas no necesitan MySQL ni que los otros servicios estén encendidos: repositorios y clientes
HTTP se simulan con Mockito, los endpoints se prueban con MockMvc y los WebClient con una respuesta simulada.

```bash
./mvnw verify
```

`verify` ejecuta las pruebas, genera el informe en `target/site/jacoco/index.html`
y **falla si la cobertura de líneas baja del 80 %**.

| Servicio | Pruebas | Cobertura de líneas |
|---|---|---|
| probadores-service | `ProbadorServiceTest`, `ProbadorControllerTest`, `ConfiguracionTest` | 100 % |
| turnos-service | `TurnoServiceTest`, `TurnoControllerTest`, `ClientesHttpTest` | 96 % |
| usuarios-service | `UsuarioServiceTest`, `UsuarioControllerTest`, `ConfiguracionTest` | 100 % |

(No se cuentan las clases `*Application`, que solo arrancan Spring.)

## URL pública para la app móvil (VS Code)

Para que el teléfono llegue al Gateway desde Internet:

1. Con todo levantado, abrir VS Code e iniciar sesión con la cuenta de GitHub.
2. Panel **Puertos** (*Ports*) → **Reenviar un puerto** → `8080`.
3. Clic derecho sobre el puerto → **Visibilidad del puerto → Pública**.
4. Copiar la dirección que entrega (por ejemplo `https://abc123-8080.brs.devtunnels.ms/`).
5. En la app, usar esa dirección como `BASE_URL` de Retrofit (terminada en `/`).

Solo se publica el Gateway: los servicios y MySQL quedan en tu computador.

## Variables de entorno

| Variable | Por defecto | Uso |
|---|---|---|
| `DB_HOST` / `DB_PORT` | `localhost` / `3306` | Servidor MySQL |
| `DB_USER` / `DB_PASSWORD` | `root` / vacío | Credenciales MySQL |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Dirección de Eureka |
| `PROBADORES_URL` / `USUARIOS_URL` | `http://probadores-service` / `http://usuarios-service` | Servicios que consume turnos (nombre en Eureka) |
