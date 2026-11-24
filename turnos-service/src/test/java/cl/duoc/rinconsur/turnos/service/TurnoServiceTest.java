package cl.duoc.rinconsur.turnos.service;

import cl.duoc.rinconsur.turnos.client.ProbadorClient;
import cl.duoc.rinconsur.turnos.client.UsuarioClient;
import cl.duoc.rinconsur.turnos.dto.CambiarEstadoRequest;
import cl.duoc.rinconsur.turnos.dto.CrearTurnoRequest;
import cl.duoc.rinconsur.turnos.dto.EsperaResponse;
import cl.duoc.rinconsur.turnos.dto.GuardarTurnoRequest;
import cl.duoc.rinconsur.turnos.dto.ParametrosDto;
import cl.duoc.rinconsur.turnos.dto.ResumenProbadoresDto;
import cl.duoc.rinconsur.turnos.dto.TurnoResponse;
import cl.duoc.rinconsur.turnos.model.EstadoTurno;
import cl.duoc.rinconsur.turnos.model.MotivoAbandono;
import cl.duoc.rinconsur.turnos.model.Parametros;
import cl.duoc.rinconsur.turnos.model.Turno;
import cl.duoc.rinconsur.turnos.repository.ParametrosRepository;
import cl.duoc.rinconsur.turnos.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Pruebas unitarias: repositorios y clientes HTTP son mocks; el reloj está fijo en el 24-11-2026 a mediodía.
class TurnoServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 24);
    private static final String RUT = "19234587-1";

    private TurnoRepository turnos;
    private ParametrosRepository parametros;
    private ProbadorClient probadorClient;
    private UsuarioClient usuarioClient;
    private TurnoService servicio;
    private long ahora;

    @BeforeEach
    void preparar() {
        turnos = mock(TurnoRepository.class);
        parametros = mock(ParametrosRepository.class);
        probadorClient = mock(ProbadorClient.class);
        usuarioClient = mock(UsuarioClient.class);
        Clock reloj = Clock.fixed(HOY.atTime(12, 0).atZone(ZONA).toInstant(), ZONA);
        ahora = reloj.millis();
        servicio = new TurnoService(turnos, parametros, probadorClient, usuarioClient, reloj);
        when(parametros.findById(Parametros.ID_UNICO)).thenReturn(Optional.empty());
        when(turnos.save(any(Turno.class))).thenAnswer(i -> i.getArgument(0));
        when(parametros.save(any(Parametros.class))).thenAnswer(i -> i.getArgument(0));
        when(turnos.findByFechaAndNumero(any(), anyInt())).thenReturn(Optional.empty());
        when(turnos.findFirstByFechaAndClienteRutAndEstadoIn(any(), anyString(), anyCollection())).thenReturn(Optional.empty());
    }

    private Turno turno(String id, int numero, boolean preferente, EstadoTurno estado) {
        return Turno.builder().id(id).numero(numero).fecha(HOY).prendas(2).preferente(preferente)
                .estado(estado).horaIngreso(0L).build();
    }

    private GuardarTurnoRequest guardar(int numero, String estado, String rut) {
        return new GuardarTurnoRequest(numero, HOY, 1L, false, 3, estado, null, null, null, "16842311-K", rut);
    }

    private int estado(Runnable accion) {
        return assertThrows(ResponseStatusException.class, accion::run).getStatusCode().value();
    }

    @Test
    @DisplayName("crear: siguiente número del día, en espera, con UUID y quién lo registró")
    void crear() {
        when(turnos.findTopByFechaOrderByNumeroDesc(HOY)).thenReturn(Optional.of(turno("a", 4, false, EstadoTurno.ATENDIDO)));

        TurnoResponse nuevo = servicio.crear(new CrearTurnoRequest(3, true, null, "16842311-k"));
        assertEquals("T-005", nuevo.codigo());
        assertEquals("EN_ESPERA", nuevo.estado());
        assertEquals("16842311-K", nuevo.registradoPor());
        assertEquals(ahora, nuevo.horaIngreso());
        assertNotNull(nuevo.id());
        assertTrue(nuevo.preferente());
    }

    @Test
    @DisplayName("crear: primer turno del día es el 1; prendas fuera del máximo 400")
    void crearPrimeroYPrendas() {
        when(turnos.findTopByFechaOrderByNumeroDesc(HOY)).thenReturn(Optional.empty());
        assertEquals(1, servicio.crear(new CrearTurnoRequest(1, null, "", null)).numero());
        assertEquals(400, estado(() -> servicio.crear(new CrearTurnoRequest(7, false, null, null))));
        assertEquals(400, estado(() -> servicio.crear(new CrearTurnoRequest(null, false, null, null))));

        // Con otro máximo guardado por la jefatura, el límite cambia.
        Parametros p = Parametros.porDefecto();
        p.setMaxPrendas(8);
        when(parametros.findById(Parametros.ID_UNICO)).thenReturn(Optional.of(p));
        assertEquals(8, servicio.crear(new CrearTurnoRequest(8, false, null, null)).prendas());
    }

    @Test
    @DisplayName("crear con RUT: debe ser cliente activo y sin otro turno abierto")
    void crearConCliente() {
        when(turnos.findTopByFechaOrderByNumeroDesc(HOY)).thenReturn(Optional.empty());
        when(usuarioClient.esClienteActivo(RUT)).thenReturn(true);

        TurnoResponse r = servicio.crear(new CrearTurnoRequest(2, false, RUT, "16842311-K"));
        assertEquals(RUT, r.clienteRut());
        assertNull(r.registradoPor());

        when(turnos.findFirstByFechaAndClienteRutAndEstadoIn(eq(HOY), eq(RUT), anyCollection()))
                .thenReturn(Optional.of(turno("x", 1, false, EstadoTurno.EN_ESPERA)));
        assertEquals(409, estado(() -> servicio.crear(new CrearTurnoRequest(2, false, RUT, null))));

        when(usuarioClient.esClienteActivo(RUT)).thenReturn(false);
        assertEquals(404, estado(() -> servicio.crear(new CrearTurnoRequest(2, false, RUT, null))));
    }

    @Test
    @DisplayName("fila, listar y turno del cliente")
    void consultas() {
        when(turnos.findByFechaAndEstadoInOrderByPreferenteDescNumeroAsc(eq(HOY), anyCollection()))
                .thenReturn(List.of(turno("a", 2, true, EstadoTurno.EN_ESPERA)));
        when(turnos.findAllByOrderByFechaAscNumeroAsc()).thenReturn(List.of(turno("a", 1, false, EstadoTurno.ATENDIDO)));
        when(turnos.findByFechaOrderByNumeroAsc(HOY)).thenReturn(List.of());

        assertEquals("T-002", servicio.fila().get(0).codigo());
        assertEquals(1, servicio.listar(null).size());
        assertTrue(servicio.listar(HOY).isEmpty());
        assertEquals(404, estado(() -> servicio.turnoDeCliente(RUT)));

        when(turnos.findFirstByFechaAndClienteRutAndEstadoIn(eq(HOY), eq(RUT), anyCollection()))
                .thenReturn(Optional.of(turno("a", 3, false, EstadoTurno.LLAMADO)));
        assertEquals("LLAMADO", servicio.turnoDeCliente(" 19234587-1 ").estado());
    }

    @Test
    @DisplayName("espera: personas antes, minutos y turno cerrado")
    void espera() {
        Turno t1 = turno("a", 1, false, EstadoTurno.EN_ESPERA);
        Turno t2 = turno("b", 2, false, EstadoTurno.EN_ESPERA);
        when(turnos.findByFechaAndNumero(HOY, 2)).thenReturn(Optional.of(t2));
        when(turnos.findByFechaAndEstadoInOrderByPreferenteDescNumeroAsc(eq(HOY), anyCollection())).thenReturn(List.of(t1, t2));
        when(probadorClient.resumen()).thenReturn(new ResumenProbadoresDto(4, 2, 0));

        EsperaResponse e = servicio.espera(2);
        assertEquals(1, e.personasAntes());
        assertEquals(8, e.minutosEstimados());

        when(turnos.findByFechaAndNumero(HOY, 1)).thenReturn(Optional.of(turno("a", 1, false, EstadoTurno.ATENDIDO)));
        assertNull(servicio.espera(1).minutosEstimados());
        assertEquals(404, estado(() -> servicio.espera(9)));
    }

    @Test
    @DisplayName("estimar minutos: sin espera si hay libre y nadie antes; rondas por probador")
    void estimar() {
        assertEquals(0, servicio.estimarMinutos(0, 1, 4));
        assertEquals(8, servicio.estimarMinutos(0, 0, 4));
        assertEquals(16, servicio.estimarMinutos(4, 0, 4));
        assertEquals(16, servicio.estimarMinutos(1, 0, 0));
    }

    @Test
    @DisplayName("cambiar estado: llamar, atender, abandono con motivo, no se presentó")
    void cambiarEstado() {
        Turno t = turno("a", 1, false, EstadoTurno.EN_ESPERA);
        when(turnos.findById("a")).thenReturn(Optional.of(t));

        assertEquals("LLAMADO", servicio.cambiarEstado("a", new CambiarEstadoRequest("llamado", null)).estado());
        assertEquals(ahora, t.getHoraLlamado());
        assertEquals(ahora, servicio.cambiarEstado("a", new CambiarEstadoRequest("ATENDIDO", null)).horaCierre());

        t.setEstado(EstadoTurno.EN_ESPERA);
        assertEquals("ESPERA_LARGA", servicio.cambiarEstado("a", new CambiarEstadoRequest("ABANDONO", "espera_larga")).motivoAbandono());
        t.setEstado(EstadoTurno.EN_ESPERA);
        assertEquals("OTRO", servicio.cambiarEstado("a", new CambiarEstadoRequest("ABANDONO", " ")).motivoAbandono());
        t.setEstado(EstadoTurno.LLAMADO);
        assertEquals(MotivoAbandono.NO_INFORMADO.name(),
                servicio.cambiarEstado("a", new CambiarEstadoRequest("NO_SE_PRESENTO", null)).motivoAbandono());
    }

    @Test
    @DisplayName("cambiar estado: cerrado 409, inexistente 404, inválido o volver a espera 400")
    void cambiarEstadoErrores() {
        Turno t = turno("a", 1, false, EstadoTurno.EN_ESPERA);
        when(turnos.findById("a")).thenReturn(Optional.of(t));
        when(turnos.findById("z")).thenReturn(Optional.empty());

        assertEquals(400, estado(() -> servicio.cambiarEstado("a", new CambiarEstadoRequest("EN_ESPERA", null))));
        assertEquals(400, estado(() -> servicio.cambiarEstado("a", new CambiarEstadoRequest("VOLANDO", null))));
        assertEquals(400, estado(() -> servicio.cambiarEstado("a", new CambiarEstadoRequest(null, null))));
        assertEquals(404, estado(() -> servicio.cambiarEstado("z", new CambiarEstadoRequest("LLAMADO", null))));
        t.setEstado(EstadoTurno.ATENDIDO);
        assertEquals(409, estado(() -> servicio.cambiarEstado("a", new CambiarEstadoRequest("LLAMADO", null))));
    }

    @Test
    @DisplayName("guardar: crea uno nuevo y actualiza uno abierto con todos sus campos")
    void guardar() {
        when(turnos.findById("n")).thenReturn(Optional.empty());
        TurnoResponse nuevo = servicio.guardar("n", guardar(3, "EN_ESPERA", null));
        assertEquals("n", nuevo.id());
        assertEquals("16842311-K", nuevo.registradoPor());

        Turno abierto = turno("a", 1, false, EstadoTurno.EN_ESPERA);
        when(turnos.findById("a")).thenReturn(Optional.of(abierto));
        when(turnos.findByFechaAndNumero(HOY, 1)).thenReturn(Optional.of(abierto));
        TurnoResponse cerrado = servicio.guardar("a", new GuardarTurnoRequest(1, HOY, 1L, true, 2, "ABANDONO",
                5L, 9L, "SE_HIZO_TARDE", null, null));
        assertEquals("SE_HIZO_TARDE", cerrado.motivoAbandono());
        assertEquals(9L, cerrado.horaCierre());

        // Volver a enviar el mismo turno cerrado sin cambiar su estado se acepta (sincronización repetida).
        assertEquals("ABANDONO", servicio.guardar("a", new GuardarTurnoRequest(1, HOY, 1L, true, 2, "ABANDONO",
                5L, 9L, "SE_HIZO_TARDE", null, null)).estado());
    }

    @Test
    @DisplayName("guardar: número repetido 409, cerrado no se reabre 409, prendas 400, cliente validado")
    void guardarErrores() {
        when(turnos.findById(anyString())).thenReturn(Optional.empty());
        when(turnos.findByFechaAndNumero(HOY, 1)).thenReturn(Optional.of(turno("otro", 1, false, EstadoTurno.EN_ESPERA)));
        assertEquals(409, estado(() -> servicio.guardar("n", guardar(1, "EN_ESPERA", null))));

        Turno cerrado = turno("c", 2, false, EstadoTurno.ATENDIDO);
        when(turnos.findById("c")).thenReturn(Optional.of(cerrado));
        assertEquals(409, estado(() -> servicio.guardar("c", guardar(2, "EN_ESPERA", null))));

        assertEquals(400, estado(() -> servicio.guardar("n", new GuardarTurnoRequest(3, HOY, 1L, false, 9, "EN_ESPERA",
                null, null, null, null, null))));

        when(usuarioClient.esClienteActivo(RUT)).thenReturn(false);
        assertEquals(404, estado(() -> servicio.guardar("n", guardar(3, "EN_ESPERA", RUT))));
        when(usuarioClient.esClienteActivo(RUT)).thenReturn(true);
        assertEquals(RUT, servicio.guardar("n", guardar(3, "EN_ESPERA", RUT)).clienteRut());
        // Un turno cerrado de un cliente no se valida contra usuarios-service.
        when(usuarioClient.esClienteActivo(RUT)).thenReturn(false);
        assertEquals("ATENDIDO", servicio.guardar("n", guardar(3, "ATENDIDO", RUT)).estado());
    }

    @Test
    @DisplayName("parámetros: por defecto, guardados y validaciones cruzadas")
    void parametros() {
        assertEquals(6, servicio.parametros().maxPrendas());

        ParametrosDto ok = new ParametrosDto(5, 10, 2, 15, 25, 2, 30000, 10, 21, 15);
        assertEquals(5, servicio.guardarParametros(ok).maxPrendas());

        assertEquals(400, estado(() -> servicio.guardarParametros(new ParametrosDto(5, 10, 2, 15, 25, 2, 30000, 21, 21, 15))));
        assertEquals(400, estado(() -> servicio.guardarParametros(new ParametrosDto(5, 10, 2, 30, 25, 2, 30000, 10, 21, 15))));
    }

    @Test
    @DisplayName("estados abiertos")
    void abiertos() {
        assertTrue(EstadoTurno.LLAMADO.abierto());
        assertFalse(EstadoTurno.NO_SE_PRESENTO.abierto());
    }
}
