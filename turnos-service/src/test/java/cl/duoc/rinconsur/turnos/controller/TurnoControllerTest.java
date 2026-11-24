package cl.duoc.rinconsur.turnos.controller;

import cl.duoc.rinconsur.turnos.config.ManejadorErrores;
import cl.duoc.rinconsur.turnos.dto.EsperaResponse;
import cl.duoc.rinconsur.turnos.dto.ParametrosDto;
import cl.duoc.rinconsur.turnos.dto.TurnoResponse;
import cl.duoc.rinconsur.turnos.service.TurnoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Prueba los endpoints REST sin levantar Spring ni MySQL: el servicio es un mock.
class TurnoControllerTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 24);
    private static final TurnoResponse T1 = new TurnoResponse("a", "T-001", 1, HOY, 0L, false, 3, "EN_ESPERA",
            null, null, null, null, null);
    private static final ParametrosDto PARAMS = new ParametrosDto(6, 8, 1, 15, 25, 2, 27990, 10, 21, 15);

    private TurnoService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(TurnoService.class);
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new TurnoController(servicio))
                .setControllerAdvice(new ManejadorErrores())
                .setValidator(validador)
                .build();
    }

    private static String json(String texto) {
        return texto.replace('\'', '"');
    }

    @Test
    @DisplayName("consultas: listar, fila, turno del cliente y espera")
    void consultas() throws Exception {
        when(servicio.listar(HOY)).thenReturn(List.of(T1));
        when(servicio.fila()).thenReturn(List.of(T1));
        when(servicio.turnoDeCliente("19234587-1")).thenReturn(T1);
        when(servicio.espera(1)).thenReturn(new EsperaResponse("T-001", "EN_ESPERA", 0, 1, 0));
        when(servicio.espera(9)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el turno 9 hoy"));

        mvc.perform(get("/api/turnos").param("fecha", "2026-11-24")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("a"));
        mvc.perform(get("/api/turnos/fila")).andExpect(status().isOk()).andExpect(jsonPath("$[0].codigo").value("T-001"));
        mvc.perform(get("/api/turnos/cliente/19234587-1")).andExpect(status().isOk());
        mvc.perform(get("/api/turnos/1/espera")).andExpect(status().isOk()).andExpect(jsonPath("$.minutosEstimados").value(0));
        mvc.perform(get("/api/turnos/9/espera")).andExpect(status().isNotFound()).andExpect(jsonPath("$.mensaje").value("No existe el turno 9 hoy"));
    }

    @Test
    @DisplayName("crear: 201, prendas faltantes o RUT mal escrito 400, JSON roto 400")
    void crear() throws Exception {
        when(servicio.crear(any())).thenReturn(T1);

        mvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(json("{'prendas':3,'preferente':false}")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(json("{'clienteRut':'abc'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.prendas").exists())
                .andExpect(jsonPath("$.campos.clienteRut").exists());
        mvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content("{roto"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("guardar completo y cambiar estado")
    void guardarYCambiar() throws Exception {
        when(servicio.guardar(eq("a"), any())).thenReturn(T1);
        when(servicio.cambiarEstado(eq("a"), any())).thenReturn(T1);

        mvc.perform(put("/api/turnos/a").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'numero':1,'fecha':'2026-11-24','horaIngreso':0,'prendas':3,'estado':'EN_ESPERA'}")))
                .andExpect(status().isOk());
        mvc.perform(put("/api/turnos/a").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'numero':1,'fecha':'2026-11-24','horaIngreso':0,'prendas':3,'estado':'PERDIDO'}")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.estado").exists());
        mvc.perform(put("/api/turnos/a/estado").contentType(MediaType.APPLICATION_JSON).content(json("{'estado':'ABANDONO','motivo':'OTRO'}")))
                .andExpect(status().isOk());
        mvc.perform(put("/api/turnos/a/estado").contentType(MediaType.APPLICATION_JSON).content(json("{'estado':'EN_ESPERA'}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("parámetros: leer, guardar y validar rangos")
    void parametros() throws Exception {
        when(servicio.parametros()).thenReturn(PARAMS);
        when(servicio.guardarParametros(any())).thenReturn(PARAMS);

        mvc.perform(get("/api/turnos/parametros")).andExpect(status().isOk()).andExpect(jsonPath("$.maxPrendas").value(6));
        mvc.perform(put("/api/turnos/parametros").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'maxPrendas':6,'tiempoReferenciaUso':8,'minutosOrdenamiento':1,'umbralProlongado':15,"
                                + "'umbralAyuda':25,'minutosPresentarse':2,'ticketPromedio':27990,'horaApertura':10,'horaCierre':21,'minutosCierreFila':15}")))
                .andExpect(status().isOk());
        mvc.perform(put("/api/turnos/parametros").contentType(MediaType.APPLICATION_JSON).content(json("{'maxPrendas':50}")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.maxPrendas").exists());
    }
}
