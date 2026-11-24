package cl.duoc.rinconsur.usuarios.service;

import cl.duoc.rinconsur.usuarios.dto.ActualizarUsuarioRequest;
import cl.duoc.rinconsur.usuarios.dto.CambiarContrasenaRequest;
import cl.duoc.rinconsur.usuarios.dto.ConfiguracionInicialRequest;
import cl.duoc.rinconsur.usuarios.dto.ContrasenaRequest;
import cl.duoc.rinconsur.usuarios.dto.CrearColaboradorRequest;
import cl.duoc.rinconsur.usuarios.dto.CuentaResponse;
import cl.duoc.rinconsur.usuarios.dto.LoginRequest;
import cl.duoc.rinconsur.usuarios.dto.RecuperarRequest;
import cl.duoc.rinconsur.usuarios.dto.RegistroClienteRequest;
import cl.duoc.rinconsur.usuarios.model.Rol;
import cl.duoc.rinconsur.usuarios.model.TurnoLaboral;
import cl.duoc.rinconsur.usuarios.model.Usuario;
import cl.duoc.rinconsur.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Pruebas del servicio: el repositorio es un mock, BCrypt es real (con pocas rondas) y el reloj es fijo.
class UsuarioServiceTest {

    private static final long AHORA = 1_800_000_000_000L;

    private UsuarioRepository repositorio;
    private BCryptPasswordEncoder encoder;
    private UsuarioService servicio;
    private Usuario jefa;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        servicio = new UsuarioService(repositorio, encoder, Clock.fixed(Instant.ofEpochMilli(AHORA), ZoneId.of("UTC")));
        jefa = Usuario.builder().id(1L).rut("12984561-9").nombre("Patricia Morales").rol(Rol.JEFATURA)
                .turnoLaboral(TurnoLaboral.MANANA).contrasenaHash(encoder.encode("clave123"))
                .recuperacionHash(encoder.encode("ABCD2345")).activo(true).creado(LocalDateTime.now()).build();
        ana = Usuario.builder().id(2L).rut("19234587-1").nombre("Ana Pérez").rol(Rol.CLIENTE)
                .contrasenaHash(encoder.encode("cliente1")).activo(true).creado(LocalDateTime.now()).build();
        when(repositorio.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(repositorio.findByRut("12984561-9")).thenReturn(Optional.of(jefa));
        when(repositorio.findByRut("19234587-1")).thenReturn(Optional.of(ana));
    }

    private int estado(Runnable accion) {
        return assertThrows(ResponseStatusException.class, accion::run).getStatusCode().value();
    }

    @Test
    @DisplayName("configuración inicial: crea la primera jefatura con código de recuperación, solo una vez")
    void configuracionInicial() {
        when(repositorio.countByRolAndActivoTrue(Rol.JEFATURA)).thenReturn(0L);
        assertFalse(servicio.estadoInicial().configurado());

        CuentaResponse cuenta = servicio.configuracionInicial(
                new ConfiguracionInicialRequest("12.984.561-9", "Patricia  Morales", "manana", "clave123"));
        assertEquals("JEFATURA", cuenta.usuario().rol());
        assertEquals("Patricia Morales", cuenta.usuario().nombre());
        assertTrue(cuenta.usuario().tieneContrasena());
        assertTrue(cuenta.codigoRecuperacion().matches("[A-Z2-9]{4}-[A-Z2-9]{4}"));

        when(repositorio.countByRolAndActivoTrue(Rol.JEFATURA)).thenReturn(1L);
        assertEquals(409, estado(() -> servicio.configuracionInicial(
                new ConfiguracionInicialRequest("12.984.561-9", "Patricia Morales", "MANANA", "clave123"))));
    }

    @Test
    @DisplayName("registro de cliente: requiere sección configurada, RUT válido y no repetido")
    void registrarCliente() {
        when(repositorio.countByRolAndActivoTrue(Rol.JEFATURA)).thenReturn(0L);
        assertEquals(409, estado(() -> servicio.registrarCliente(new RegistroClienteRequest("20118436-3", "Luis Soto", "cliente1"))));

        when(repositorio.countByRolAndActivoTrue(Rol.JEFATURA)).thenReturn(1L);
        CuentaResponse cuenta = servicio.registrarCliente(new RegistroClienteRequest("20.118.436-3", "Luis Soto", "cliente1"));
        assertEquals("CLIENTE", cuenta.usuario().rol());
        assertNull(cuenta.usuario().turnoLaboral());

        assertEquals(400, estado(() -> servicio.registrarCliente(new RegistroClienteRequest("20118436-4", "Luis Soto", "cliente1"))));
        when(repositorio.existsByRut("20118436-3")).thenReturn(true);
        assertEquals(409, estado(() -> servicio.registrarCliente(new RegistroClienteRequest("20118436-3", "Luis Soto", "cliente1"))));
    }

    @Test
    @DisplayName("la jefatura crea colaboradores sin contraseña; no cuentas de cliente ni roles inexistentes")
    void crearColaborador() {
        var nuevo = servicio.crearColaborador(new CrearColaboradorRequest("17550982-8", "Javiera Muñoz", "vendedor", "TARDE"));
        assertEquals("VENDEDOR", nuevo.rol());
        assertEquals("TARDE", nuevo.turnoLaboral());
        assertFalse(nuevo.tieneContrasena());

        assertEquals(400, estado(() -> servicio.crearColaborador(new CrearColaboradorRequest("17550982-8", "Javiera Muñoz", "CLIENTE", "TARDE"))));
        assertEquals(400, estado(() -> servicio.crearColaborador(new CrearColaboradorRequest("17550982-8", "Javiera Muñoz", "GERENTE", "TARDE"))));
        assertEquals(400, estado(() -> servicio.crearColaborador(new CrearColaboradorRequest("17550982-8", "Javiera Muñoz", "VENDEDOR", "NOCHE"))));
    }

    @Test
    @DisplayName("listar filtra por rol y busca por nombre o RUT; obtener 404")
    void listarYObtener() {
        when(repositorio.findAllByOrderByNombreAsc()).thenReturn(List.of(ana, jefa));
        when(repositorio.findByRolOrderByNombreAsc(Rol.CLIENTE)).thenReturn(List.of(ana));

        assertEquals(2, servicio.listar(null, null).size());
        assertEquals(1, servicio.listar("cliente", "").size());
        assertEquals("Patricia Morales", servicio.listar(null, "morales").get(0).nombre());
        assertEquals("Ana Pérez", servicio.listar(null, "19.234").get(0).nombre());
        assertTrue(servicio.listar(null, "nadie").isEmpty());
        assertEquals(400, estado(() -> servicio.listar("GERENTE", null)));
        assertEquals("Ana Pérez", servicio.obtener("19.234.587-1").nombre());
        assertEquals(404, estado(() -> servicio.obtener("11111111-1")));
    }

    @Test
    @DisplayName("actualizar: nombre de cualquiera; perfil y turno solo de colaboradores; siempre queda una jefatura")
    void actualizar() {
        assertEquals("Ana María Pérez", servicio.actualizar("19234587-1", new ActualizarUsuarioRequest("Ana  María Pérez", null, null, null)).nombre());
        assertEquals(400, estado(() -> servicio.actualizar("19234587-1", new ActualizarUsuarioRequest(null, "VENDEDOR", null, null))));

        when(repositorio.countByRolAndActivoTrue(Rol.JEFATURA)).thenReturn(1L);
        assertEquals(409, estado(() -> servicio.actualizar("12984561-9", new ActualizarUsuarioRequest(null, "ASISTENTE", null, null))));
        assertEquals(409, estado(() -> servicio.actualizar("12984561-9", new ActualizarUsuarioRequest(null, null, null, false))));

        when(repositorio.countByRolAndActivoTrue(Rol.JEFATURA)).thenReturn(2L);
        var editada = servicio.actualizar("12984561-9", new ActualizarUsuarioRequest(null, "ASISTENTE", "TARDE", false));
        assertEquals("ASISTENTE", editada.rol());
        assertEquals("TARDE", editada.turnoLaboral());
        assertFalse(editada.activo());
    }

    @Test
    @DisplayName("login: correcto, incorrecto con intentos restantes, inactiva y sin contraseña")
    void iniciarSesion() {
        assertEquals("JEFATURA", servicio.iniciarSesion(new LoginRequest("12.984.561-9", "clave123")).rol());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarSesion(new LoginRequest("12984561-9", "mala999")));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        assertTrue(e.getReason().contains("Le quedan 4 intentos"));
        assertEquals(401, estado(() -> servicio.iniciarSesion(new LoginRequest("11111111-1", "x"))));

        jefa.setActivo(false);
        assertEquals(403, estado(() -> servicio.iniciarSesion(new LoginRequest("12984561-9", "clave123"))));
        jefa.setActivo(true);
        jefa.setContrasenaHash(null);
        assertEquals(409, estado(() -> servicio.iniciarSesion(new LoginRequest("12984561-9", "clave123"))));
    }

    @Test
    @DisplayName("5 intentos fallidos bloquean la cuenta (423)")
    void bloqueo() {
        jefa.setIntentosFallidos(3);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarSesion(new LoginRequest("12984561-9", "mala999")));
        assertTrue(e.getReason().contains("Le queda 1 intento."));

        assertEquals(423, estado(() -> servicio.iniciarSesion(new LoginRequest("12984561-9", "mala999"))));
        assertEquals(AHORA + UsuarioService.MILIS_BLOQUEO, jefa.getBloqueadaHasta());
        // Aunque ahora use la contraseña correcta, sigue bloqueada.
        assertEquals(423, estado(() -> servicio.iniciarSesion(new LoginRequest("12984561-9", "clave123"))));
    }

    @Test
    @DisplayName("primera contraseña del colaborador, solo si no tenía")
    void crearContrasena() {
        jefa.setContrasenaHash(null);
        CuentaResponse cuenta = servicio.crearContrasena("12984561-9", new ContrasenaRequest("nueva123"));
        assertTrue(encoder.matches("nueva123", jefa.getContrasenaHash()));
        assertTrue(encoder.matches(cuenta.codigoRecuperacion().replace("-", ""), jefa.getRecuperacionHash()));

        assertEquals(409, estado(() -> servicio.crearContrasena("12984561-9", new ContrasenaRequest("otra1234"))));
    }

    @Test
    @DisplayName("cambiar contraseña: exige la actual, que sea distinta y entrega un código nuevo")
    void cambiarContrasena() {
        assertEquals(401, estado(() -> servicio.cambiarContrasena("19234587-1", new CambiarContrasenaRequest("mala999", "cliente2"))));
        assertEquals(400, estado(() -> servicio.cambiarContrasena("19234587-1", new CambiarContrasenaRequest("cliente1", "cliente1"))));

        CuentaResponse cuenta = servicio.cambiarContrasena("19234587-1", new CambiarContrasenaRequest("cliente1", "cliente2"));
        assertTrue(encoder.matches("cliente2", ana.getContrasenaHash()));
        assertTrue(cuenta.codigoRecuperacion().length() == 9);

        ana.setContrasenaHash(null);
        assertEquals(409, estado(() -> servicio.cambiarContrasena("19234587-1", new CambiarContrasenaRequest("a", "cliente3"))));
    }

    @Test
    @DisplayName("recuperar con el código (con o sin guion, en minúsculas)")
    void recuperar() {
        assertEquals(401, estado(() -> servicio.recuperar("12984561-9", new RecuperarRequest("ZZZZ-ZZZZ", "nueva456"))));

        servicio.recuperar("12984561-9", new RecuperarRequest("abcd-2345", "nueva456"));
        assertTrue(encoder.matches("nueva456", jefa.getContrasenaHash()));

        ana.setRecuperacionHash(null);
        assertEquals(409, estado(() -> servicio.recuperar("19234587-1", new RecuperarRequest("ABCD2345", "nueva456"))));
    }

    @Test
    @DisplayName("restablecer borra la contraseña y el código")
    void restablecer() {
        jefa.setIntentosFallidos(2);
        servicio.restablecerContrasena("12984561-9");
        assertNull(jefa.getContrasenaHash());
        assertNull(jefa.getRecuperacionHash());
        assertEquals(0, jefa.getIntentosFallidos());
    }
}
