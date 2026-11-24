package cl.duoc.rinconsur.probadores.service;

import cl.duoc.rinconsur.probadores.dto.CambiarEstadoRequest;
import cl.duoc.rinconsur.probadores.dto.CambioEstadoDto;
import cl.duoc.rinconsur.probadores.dto.CrearProbadorRequest;
import cl.duoc.rinconsur.probadores.dto.GuardarProbadorRequest;
import cl.duoc.rinconsur.probadores.dto.ProbadorResponse;
import cl.duoc.rinconsur.probadores.dto.ResumenResponse;
import cl.duoc.rinconsur.probadores.dto.UsoDto;
import cl.duoc.rinconsur.probadores.model.CambioEstado;
import cl.duoc.rinconsur.probadores.model.EstadoProbador;
import cl.duoc.rinconsur.probadores.model.Probador;
import cl.duoc.rinconsur.probadores.model.TipoProbador;
import cl.duoc.rinconsur.probadores.model.Uso;
import cl.duoc.rinconsur.probadores.repository.CambioEstadoRepository;
import cl.duoc.rinconsur.probadores.repository.ProbadorRepository;
import cl.duoc.rinconsur.probadores.repository.UsoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas del servicio con los repositorios simulados (sin MySQL) y el reloj fijo.
class ProbadorServiceTest {

    private static final long AHORA = 1_800_000_000_000L;

    private ProbadorRepository probadores;
    private UsoRepository usos;
    private CambioEstadoRepository cambios;
    private ProbadorService servicio;
    private Probador p01;

    @BeforeEach
    void preparar() {
        probadores = mock(ProbadorRepository.class);
        usos = mock(UsoRepository.class);
        cambios = mock(CambioEstadoRepository.class);
        servicio = new ProbadorService(probadores, usos, cambios, Clock.fixed(Instant.ofEpochMilli(AHORA), ZoneId.of("UTC")));
        p01 = Probador.builder().id(1L).codigo("P-01").tipo(TipoProbador.ESTANDAR).estado(EstadoProbador.LIBRE).desde(0L).build();
        when(probadores.findByCodigo("P-01")).thenReturn(Optional.of(p01));
        when(probadores.save(any(Probador.class))).thenAnswer(i -> i.getArgument(0));
        when(usos.save(any(Uso.class))).thenAnswer(i -> i.getArgument(0));
    }

    private int estado(Runnable accion) {
        return assertThrows(ResponseStatusException.class, accion::run).getStatusCode().value();
    }

    private GuardarProbadorRequest guardar(String estado) {
        return new GuardarProbadorRequest("ESTANDAR", estado, AHORA, "u1", "Cortina rota", "/foto.jpg");
    }

    @Test
    @DisplayName("listar con y sin filtro; estado inválido 400; obtener 404")
    void listarYObtener() {
        when(probadores.findAllByOrderByCodigoAsc()).thenReturn(List.of(p01));
        when(probadores.findByEstadoOrderByCodigoAsc(EstadoProbador.LIBRE)).thenReturn(List.of(p01));

        assertEquals("P-01", servicio.listar(null).get(0).codigo());
        assertEquals(1, servicio.listar("libre").size());
        assertEquals(400, estado(() -> servicio.listar("ROTO")));
        assertEquals("LIBRE", servicio.obtener("P-01").estado());
        assertEquals(404, estado(() -> servicio.obtener("P-99")));
    }

    @Test
    @DisplayName("el resumen descuenta los fuera de servicio de los habilitados")
    void resumen() {
        when(probadores.count()).thenReturn(8L);
        when(probadores.countByEstado(EstadoProbador.FUERA_DE_SERVICIO)).thenReturn(1L);
        when(probadores.countByEstado(EstadoProbador.LIBRE)).thenReturn(3L);

        assertEquals(new ResumenResponse(8, 7, 3), servicio.resumen());
    }

    @Test
    @DisplayName("crear usa el primer código libre; tipo por defecto estándar; máximo 30")
    void crear() {
        Probador p03 = Probador.builder().codigo("P-03").build();
        when(probadores.findAll()).thenReturn(List.of(p01, p03));

        ProbadorResponse nuevo = servicio.crear(new CrearProbadorRequest(null));
        assertEquals("P-02", nuevo.codigo());
        assertEquals("ESTANDAR", nuevo.tipo());
        assertEquals(AHORA, nuevo.desde());
        assertEquals("ACCESIBLE", servicio.crear(new CrearProbadorRequest("accesible")).tipo());

        when(probadores.findAll()).thenReturn(java.util.Collections.nCopies(30, p01));
        assertEquals(409, estado(() -> servicio.crear(new CrearProbadorRequest("ESTANDAR"))));
    }

    @Test
    @DisplayName("guardar crea un probador nuevo y limpia los campos que no aplican al estado")
    void guardarNuevo() {
        when(probadores.findByCodigo("P-02")).thenReturn(Optional.empty());

        ProbadorResponse r = servicio.guardar("P-02", guardar("LIBRE"));
        assertEquals("P-02", r.codigo());
        assertNull(r.usoActivoId());
        assertNull(r.motivoFueraServicio());

        assertEquals(400, estado(() -> servicio.guardar("X1", guardar("LIBRE"))));
        when(probadores.count()).thenReturn(30L);
        assertEquals(409, estado(() -> servicio.guardar("P-02", guardar("LIBRE"))));
        assertEquals(400, estado(() -> servicio.guardar("P-01", new GuardarProbadorRequest("GRANDE", "LIBRE", 0L, null, null, null))));
    }

    @Test
    @DisplayName("guardar valida la transición de estado y guarda uso activo o motivo según corresponda")
    void guardarTransiciones() {
        ProbadorResponse ocupado = servicio.guardar("P-01", guardar("OCUPADO"));
        assertEquals("u1", ocupado.usoActivoId());

        p01.setEstado(EstadoProbador.LIBRE);
        ProbadorResponse fuera = servicio.guardar("P-01", guardar("FUERA_DE_SERVICIO"));
        assertEquals("Cortina rota", fuera.motivoFueraServicio());
        assertEquals("/foto.jpg", fuera.fotoEvidencia());

        p01.setEstado(EstadoProbador.LIBRE);
        assertEquals(409, estado(() -> servicio.guardar("P-01", guardar("EN_ORDENAMIENTO"))));
        // Mismo estado (por ejemplo, solo cambia el tipo): se acepta.
        assertEquals("LIBRE", servicio.guardar("P-01", guardar("LIBRE")).estado());
    }

    @Test
    @DisplayName("cambiar estado: válido 200, inválido 409")
    void cambiarEstado() {
        assertEquals("OCUPADO", servicio.cambiarEstado("P-01", new CambiarEstadoRequest("OCUPADO")).estado());
        assertEquals(AHORA, p01.getDesde());
        assertEquals(409, estado(() -> servicio.cambiarEstado("P-01", new CambiarEstadoRequest("LIBRE"))));
    }

    @Test
    @DisplayName("eliminar: solo libre, sin usos y si queda al menos uno")
    void eliminar() {
        when(probadores.count()).thenReturn(1L);
        assertEquals(409, estado(() -> servicio.eliminar("P-01")));

        when(probadores.count()).thenReturn(3L);
        p01.setEstado(EstadoProbador.OCUPADO);
        assertEquals(409, estado(() -> servicio.eliminar("P-01")));

        p01.setEstado(EstadoProbador.LIBRE);
        when(usos.existsByCodigoProbador("P-01")).thenReturn(true);
        assertEquals(409, estado(() -> servicio.eliminar("P-01")));
        verify(probadores, never()).delete(any());

        when(usos.existsByCodigoProbador("P-01")).thenReturn(false);
        servicio.eliminar("P-01");
        verify(cambios).deleteByCodigoProbador("P-01");
        verify(probadores).delete(p01);
    }

    @Test
    @DisplayName("usos: se guardan, se listan y se validan prendas, fechas, id y probador")
    void usos() {
        UsoDto uso = new UsoDto("u1", "P-01", "t1", 100L, 200L, 3, 1, "16842311-K", "16842311-K", false);
        assertEquals("u1", servicio.guardarUso("u1", uso).id());

        assertEquals(400, estado(() -> servicio.guardarUso("otro", uso)));
        assertEquals(400, estado(() -> servicio.guardarUso("u1",
                new UsoDto("u1", "P-01", null, 100L, null, 2, 3, "1-9", null, null))));
        assertEquals(400, estado(() -> servicio.guardarUso("u1",
                new UsoDto("u1", "P-01", null, 100L, 50L, 2, 1, "1-9", null, null))));
        assertEquals(404, estado(() -> servicio.guardarUso("u1",
                new UsoDto("u1", "P-99", null, 100L, null, 2, null, "1-9", null, null))));

        when(usos.findAllByOrderByInicioAsc()).thenReturn(List.of(Uso.builder().id("u1").codigoProbador("P-01")
                .inicio(100L).prendasIngresadas(3).registroInicio("1-9").conAtraso(true).build()));
        assertTrue(servicio.usos().get(0).conAtraso());
    }

    @Test
    @DisplayName("historial: registra cambios sin duplicarlos y los lista")
    void historial() {
        CambioEstadoDto cambio = new CambioEstadoDto("P-01", "LIBRE", "OCUPADO", 100L, "16842311-K", null);
        servicio.registrarCambio(cambio);
        verify(cambios).save(any(CambioEstado.class));

        when(cambios.existsByCodigoProbadorAndFechaHoraAndNuevo("P-01", 100L, EstadoProbador.OCUPADO)).thenReturn(true);
        servicio.registrarCambio(cambio);
        verify(cambios).save(any(CambioEstado.class));

        when(cambios.findAllByOrderByFechaHoraAsc()).thenReturn(List.of(CambioEstado.builder().codigoProbador("P-01")
                .anterior(EstadoProbador.LIBRE).nuevo(EstadoProbador.OCUPADO).fechaHora(100L).colaborador("1-9").build()));
        assertEquals("OCUPADO", servicio.cambios().get(0).nuevo());
    }

    @Test
    @DisplayName("las transiciones de estado son las mismas que en la app")
    void transiciones() {
        assertTrue(EstadoProbador.LIBRE.puedePasarA(EstadoProbador.OCUPADO));
        assertTrue(EstadoProbador.OCUPADO.puedePasarA(EstadoProbador.EN_ORDENAMIENTO));
        assertTrue(EstadoProbador.EN_ORDENAMIENTO.puedePasarA(EstadoProbador.LIBRE));
        assertTrue(EstadoProbador.FUERA_DE_SERVICIO.puedePasarA(EstadoProbador.LIBRE));
        assertFalse(EstadoProbador.FUERA_DE_SERVICIO.puedePasarA(EstadoProbador.OCUPADO));
        assertFalse(EstadoProbador.LIBRE.puedePasarA(EstadoProbador.EN_ORDENAMIENTO));
    }
}
