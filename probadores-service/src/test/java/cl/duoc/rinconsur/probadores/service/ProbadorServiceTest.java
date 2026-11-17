package cl.duoc.rinconsur.probadores.service;

import cl.duoc.rinconsur.probadores.dto.CambiarEstadoRequest;
import cl.duoc.rinconsur.probadores.dto.CrearProbadorRequest;
import cl.duoc.rinconsur.probadores.dto.ProbadorResponse;
import cl.duoc.rinconsur.probadores.dto.ResumenResponse;
import cl.duoc.rinconsur.probadores.model.EstadoProbador;
import cl.duoc.rinconsur.probadores.model.Probador;
import cl.duoc.rinconsur.probadores.model.TipoProbador;
import cl.duoc.rinconsur.probadores.repository.ProbadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Pruebas unitarias del servicio: el repositorio es un mock, no se usa MySQL ni Spring.
@ExtendWith(MockitoExtension.class)
class ProbadorServiceTest {

    @Mock
    private ProbadorRepository probadorRepository;

    @InjectMocks
    private ProbadorService probadorService;

    private Probador p01;

    @BeforeEach
    void preparar() {
        p01 = Probador.builder().id(1L).codigo("P-01").tipo(TipoProbador.ESTANDAR).estado(EstadoProbador.LIBRE).build();
    }

    @Test
    @DisplayName("listar sin filtro devuelve todos ordenados por código")
    void listarTodos() {
        when(probadorRepository.findAllByOrderByCodigoAsc()).thenReturn(List.of(p01));

        List<ProbadorResponse> lista = probadorService.listar(null);

        assertEquals(1, lista.size());
        assertEquals("P-01", lista.get(0).codigo());
        assertEquals("LIBRE", lista.get(0).estado());
    }

    @Test
    @DisplayName("listar con filtro de estado usa la consulta por estado")
    void listarPorEstado() {
        when(probadorRepository.findByEstadoOrderByCodigoAsc(EstadoProbador.LIBRE)).thenReturn(List.of(p01));

        assertEquals(1, probadorService.listar("libre").size());
    }

    @Test
    @DisplayName("un estado inexistente responde 400")
    void estadoInvalido() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> probadorService.listar("ROTO"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    @DisplayName("obtener un código que no existe responde 404")
    void obtenerNoExiste() {
        when(probadorRepository.findByCodigo("P-99")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> probadorService.obtener("P-99"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    @DisplayName("el resumen descuenta los fuera de servicio de los habilitados")
    void resumen() {
        when(probadorRepository.count()).thenReturn(8L);
        when(probadorRepository.countByEstado(EstadoProbador.FUERA_DE_SERVICIO)).thenReturn(1L);
        when(probadorRepository.countByEstado(EstadoProbador.LIBRE)).thenReturn(3L);

        assertEquals(new ResumenResponse(8, 7, 3), probadorService.resumen());
    }

    @Test
    @DisplayName("crear usa el primer código libre")
    void crearPrimerCodigoLibre() {
        Probador p03 = Probador.builder().codigo("P-03").tipo(TipoProbador.ESTANDAR).estado(EstadoProbador.LIBRE).build();
        when(probadorRepository.findAll()).thenReturn(List.of(p01, p03));
        when(probadorRepository.save(any(Probador.class))).thenAnswer(i -> i.getArgument(0));

        ProbadorResponse nuevo = probadorService.crear(new CrearProbadorRequest("accesible"));

        assertEquals("P-02", nuevo.codigo());
        assertEquals("ACCESIBLE", nuevo.tipo());
        assertEquals("LIBRE", nuevo.estado());
    }

    @Test
    @DisplayName("una transición válida cambia el estado")
    void cambiarEstadoValido() {
        when(probadorRepository.findByCodigo("P-01")).thenReturn(Optional.of(p01));
        when(probadorRepository.save(any(Probador.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("OCUPADO", probadorService.cambiarEstado("P-01", new CambiarEstadoRequest("OCUPADO")).estado());
    }

    @Test
    @DisplayName("una transición inválida responde 409 y no guarda")
    void cambiarEstadoInvalido() {
        when(probadorRepository.findByCodigo("P-01")).thenReturn(Optional.of(p01));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> probadorService.cambiarEstado("P-01", new CambiarEstadoRequest("EN_ORDENAMIENTO")));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(probadorRepository, never()).save(any());
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
