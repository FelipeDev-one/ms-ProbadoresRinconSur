# Probadores Rincón Sur — Microservicios

Dos microservicios Spring Boot 4.1 (JDK 21, Maven, JPA, MySQL) para la app Probadores Rincón Sur.
Siguen la misma estructura vista en clases: `model`, `dto` (records), `repository`, `service`,
`controller`, `config` y, en turnos, un `client` con **WebClient** para hablar con el otro servicio.

| Servicio | Puerto | Base de datos | Responsabilidad |
|---|---|---|---|
| `probadores-service` | 8081 | `rinconsur_probadores` | Probadores de la sección y su estado |
| `turnos-service` | 8082 | `rinconsur_turnos` | Fila de espera y tiempo estimado. Consulta a `probadores-service` |

Cada servicio tiene su propia base de datos: `turnos-service` no guarda probadores, le pide a
`probadores-service` cuántos hay libres y habilitados para estimar la espera.

## Cómo levantarlos

1. Iniciar MySQL (Laragon, XAMPP o MySQL Server) en el puerto 3306, usuario `root` sin contraseña.
   Las bases de datos se crean solas (`createDatabaseIfNotExist=true`).
   Si tu MySQL usa otra clave, define la variable de entorno `DB_PASSWORD` o cámbiala en `application.properties`.
2. Ejecutar **primero** `probadores-service` (`ProbadoresServiceApplication`).
3. Ejecutar `turnos-service` (`TurnosServiceApplication`).

Desde la terminal, dentro de cada carpeta:

```bash
./mvnw spring-boot:run
```

Al iniciar por primera vez, `probadores-service` crea 8 probadores libres (P-01 a P-07 estándar y P-08 accesible).

## Pruebas unitarias

Prueban la lógica de cada servicio con **JUnit 5 + Mockito**: el repositorio y el cliente HTTP son
simulados, así que **no necesitan MySQL ni que el otro servicio esté encendido**.

```bash
./mvnw test
```

| Servicio | Clase | Qué prueba |
|---|---|---|
| probadores-service | `ProbadorServiceTest` | Listar y filtrar, 404, resumen, código libre al crear, transiciones de estado |
| turnos-service | `TurnoServiceTest` | Número del día, prendas 1 a 6, un turno abierto por cliente, cálculo de espera, turno cerrado no se reabre |

## Endpoints

### probadores-service — `http://localhost:8081`

| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| GET | `/api/probadores?estado=LIBRE` | — | Lista (el filtro es opcional) |
| GET | `/api/probadores/resumen` | — | `{"total","habilitados","libres"}` |
| GET | `/api/probadores/{codigo}` | — | Probador · 404 |
| POST | `/api/probadores` | `{"tipo":"ESTANDAR"}` o `"ACCESIBLE"` | 201 con el primer código libre · 409 si ya hay 30 |
| PUT | `/api/probadores/{codigo}/estado` | `{"estado":"OCUPADO"}` | 200 · 400 estado no válido · 409 transición no permitida |

Estados: `LIBRE → OCUPADO → EN_ORDENAMIENTO → LIBRE`; cualquiera puede pasar a `FUERA_DE_SERVICIO`, que solo vuelve a `LIBRE`.

### turnos-service — `http://localhost:8082`

| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| GET | `/api/turnos/fila` | — | Turnos abiertos de hoy (preferentes primero) |
| POST | `/api/turnos` | `{"prendas":3,"preferente":false,"clienteRut":"19234587-1"}` | 201 · 400 prendas fuera de 1 a 6 · 409 el cliente ya tiene turno |
| GET | `/api/turnos/{numero}/espera` | — | `{"codigo","estado","personasAntes","probadoresLibres","minutosEstimados"}` · 404 · 503 si probadores-service no responde |
| PUT | `/api/turnos/{id}/estado` | `{"estado":"LLAMADO"}` (`ATENDIDO`, `ABANDONO`) | 200 · 409 si el turno ya estaba cerrado |

`clienteRut` es opcional: va vacío cuando el turno lo registra el personal en el mesón.

## Variables de entorno (para Docker)

Las propiedades usan valores por defecto para trabajar en local, y se pueden cambiar sin tocar el código:

| Variable | Por defecto | En Docker |
|---|---|---|
| `DB_HOST` | `localhost` | `mysql` |
| `DB_USER` / `DB_PASSWORD` | `root` / vacío | los del contenedor MySQL |
| `PROBADORES_URL` (solo turnos) | `http://localhost:8081` | `http://probadores-service:8081` |
