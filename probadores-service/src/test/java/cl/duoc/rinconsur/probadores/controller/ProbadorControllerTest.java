package cl.duoc.rinconsur.probadores.controller;

import cl.duoc.rinconsur.probadores.config.ManejadorErrores;
import cl.duoc.rinconsur.probadores.dto.CambioEstadoDto;
import cl.duoc.rinconsur.probadores.dto.ProbadorResponse;
import cl.duoc.rinconsur.probadores.dto.ResumenResponse;
import cl.duoc.rinconsur.probadores.dto.UsoDto;
import cl.duoc.rinconsur.probadores.service.ProbadorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Prueba los endpoints REST sin levantar Spring ni MySQL: el servicio es un mock.
class ProbadorControllerTest {

    private static final ProbadorResponse P01 = new ProbadorResponse("P-01", "ESTANDAR", "LIBRE", 0L, null, null, null);

    private ProbadorService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(ProbadorService.class);
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ProbadorController(servicio))
                .setControllerAdvice(new ManejadorErrores())
                .setValidator(validador)
                .build();
    }

    private static String json(String texto) {
        return texto.replace('\'', '"');
    }

    @Test
    @DisplayName("listar, resumen, obtener (200 y 404)")
    void consultas() throws Exception {
        when(servicio.listar("LIBRE")).thenReturn(List.of(P01));
        when(servicio.resumen()).thenReturn(new ResumenResponse(8, 7, 3));
        when(servicio.obtener("P-01")).thenReturn(P01);
        when(servicio.obtener("P-99")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el probador P-99"));

        mvc.perform(get("/api/probadores").param("estado", "LIBRE")).andExpect(status().isOk()).andExpect(jsonPath("$[0].codigo").value("P-01"));
        mvc.perform(get("/api/probadores/resumen")).andExpect(status().isOk()).andExpect(jsonPath("$.libres").value(3));
        mvc.perform(get("/api/probadores/P-01")).andExpect(status().isOk());
        mvc.perform(get("/api/probadores/P-99")).andExpect(status().isNotFound()).andExpect(jsonPath("$.mensaje").value("No existe el probador P-99"));
    }

    @Test
    @DisplayName("crear (201) y tipo inválido (400)")
    void crear() throws Exception {
        when(servicio.crear(any())).thenReturn(P01);

        mvc.perform(post("/api/probadores").contentType(MediaType.APPLICATION_JSON).content(json("{'tipo':'ACCESIBLE'}")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/probadores").contentType(MediaType.APPLICATION_JSON).content(json("{'tipo':'GRANDE'}")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.tipo").exists());
    }

    @Test
    @DisplayName("guardar completo (200), con datos faltantes (400) y cambiar estado")
    void guardarYCambiar() throws Exception {
        when(servicio.guardar(eq("P-01"), any())).thenReturn(P01);
        when(servicio.cambiarEstado(eq("P-01"), any())).thenReturn(P01);

        mvc.perform(put("/api/probadores/P-01").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'tipo':'ESTANDAR','estado':'LIBRE','desde':0}")))
                .andExpect(status().isOk());
        mvc.perform(put("/api/probadores/P-01").contentType(MediaType.APPLICATION_JSON).content(json("{'tipo':'ESTANDAR'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.estado").exists())
                .andExpect(jsonPath("$.campos.desde").exists());
        mvc.perform(put("/api/probadores/P-01/estado").contentType(MediaType.APPLICATION_JSON).content(json("{'estado':'OCUPADO'}")))
                .andExpect(status().isOk());
        mvc.perform(put("/api/probadores/P-01/estado").contentType(MediaType.APPLICATION_JSON).content(json("{'estado':''}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("eliminar (204)")
    void eliminar() throws Exception {
        mvc.perform(delete("/api/probadores/P-01")).andExpect(status().isNoContent());
        verify(servicio).eliminar("P-01");
    }

    @Test
    @DisplayName("usos y cambios: listar, guardar y validar")
    void usosYCambios() throws Exception {
        UsoDto uso = new UsoDto("u1", "P-01", null, 100L, null, 3, null, "1-9", null, false);
        CambioEstadoDto cambio = new CambioEstadoDto("P-01", "LIBRE", "OCUPADO", 100L, "1-9", null);
        when(servicio.usos()).thenReturn(List.of(uso));
        when(servicio.guardarUso(eq("u1"), any())).thenReturn(uso);
        when(servicio.cambios()).thenReturn(List.of(cambio));
        when(servicio.registrarCambio(any())).thenReturn(cambio);

        mvc.perform(get("/api/probadores/usos")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("u1"));
        mvc.perform(put("/api/probadores/usos/u1").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'id':'u1','codigoProbador':'P-01','inicio':100,'prendasIngresadas':3,'registroInicio':'1-9'}")))
                .andExpect(status().isOk());
        mvc.perform(put("/api/probadores/usos/u1").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'id':'u1','codigoProbador':'P-01','inicio':100,'prendasIngresadas':0,'registroInicio':'1-9'}")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.prendasIngresadas").exists());
        mvc.perform(get("/api/probadores/cambios")).andExpect(status().isOk());
        mvc.perform(post("/api/probadores/cambios").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'codigoProbador':'P-01','anterior':'LIBRE','nuevo':'OCUPADO','fechaHora':100,'colaborador':'1-9'}")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("un JSON mal formado responde 400")
    void jsonInvalido() throws Exception {
        mvc.perform(post("/api/probadores").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }
}
