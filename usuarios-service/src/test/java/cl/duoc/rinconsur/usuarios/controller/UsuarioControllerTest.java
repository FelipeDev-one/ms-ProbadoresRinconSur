package cl.duoc.rinconsur.usuarios.controller;

import cl.duoc.rinconsur.usuarios.config.ManejadorErrores;
import cl.duoc.rinconsur.usuarios.dto.CuentaResponse;
import cl.duoc.rinconsur.usuarios.dto.EstadoInicialResponse;
import cl.duoc.rinconsur.usuarios.dto.UsuarioResponse;
import cl.duoc.rinconsur.usuarios.service.UsuarioService;
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
class UsuarioControllerTest {

    private static final UsuarioResponse ANA = new UsuarioResponse(2L, "19234587-1", "Ana Pérez", "CLIENTE", null, true, true);
    private static final CuentaResponse CUENTA = new CuentaResponse(ANA, "ABCD-2345");

    private UsuarioService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(UsuarioService.class);
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new UsuarioController(servicio))
                .setControllerAdvice(new ManejadorErrores())
                .setValidator(validador)
                .build();
    }

    private static String json(String texto) {
        return texto.replace('\'', '"');
    }

    @Test
    @DisplayName("estado inicial y configuración inicial")
    void configuracion() throws Exception {
        when(servicio.estadoInicial()).thenReturn(new EstadoInicialResponse(false));
        when(servicio.configuracionInicial(any())).thenReturn(CUENTA);

        mvc.perform(get("/api/usuarios/estado-inicial")).andExpect(status().isOk()).andExpect(jsonPath("$.configurado").value(false));
        mvc.perform(post("/api/usuarios/configuracion-inicial").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'rut':'12.984.561-9','nombre':'Patricia Morales','turnoLaboral':'MANANA','contrasena':'clave123'}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoRecuperacion").value("ABCD-2345"));
    }

    @Test
    @DisplayName("listar, obtener y registro (201) con validaciones (400)")
    void cuentas() throws Exception {
        when(servicio.listar("CLIENTE", null)).thenReturn(List.of(ANA));
        when(servicio.obtener("19234587-1")).thenReturn(ANA);
        when(servicio.registrarCliente(any())).thenReturn(CUENTA);

        mvc.perform(get("/api/usuarios").param("rol", "CLIENTE")).andExpect(status().isOk()).andExpect(jsonPath("$[0].rut").value("19234587-1"));
        mvc.perform(get("/api/usuarios/19234587-1")).andExpect(status().isOk()).andExpect(jsonPath("$.contrasenaHash").doesNotExist());
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'rut':'19.234.587-1','nombre':'Ana Pérez','contrasena':'cliente1'}")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'rut':'abc','nombre':'Ana','contrasena':'123'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.rut").exists())
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.contrasena").exists());
    }

    @Test
    @DisplayName("crear y editar colaboradores")
    void colaboradores() throws Exception {
        UsuarioResponse javiera = new UsuarioResponse(3L, "17550982-8", "Javiera Muñoz", "VENDEDOR", "TARDE", true, false);
        when(servicio.crearColaborador(any())).thenReturn(javiera);
        when(servicio.actualizar(eq("17550982-8"), any())).thenReturn(javiera);

        mvc.perform(post("/api/usuarios/colaboradores").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'rut':'17.550.982-8','nombre':'Javiera Muñoz','rol':'VENDEDOR','turnoLaboral':'TARDE'}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tieneContrasena").value(false));
        mvc.perform(put("/api/usuarios/17550982-8").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'rol':'ASISTENTE','activo':false}")))
                .andExpect(status().isOk());
        mvc.perform(put("/api/usuarios/17550982-8").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'rol':'GERENTE'}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login: 200, 401 y 423")
    void login() throws Exception {
        when(servicio.iniciarSesion(any()))
                .thenReturn(ANA)
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contraseña incorrecta"))
                .thenThrow(new ResponseStatusException(HttpStatus.LOCKED, "Demasiados intentos"));
        String cuerpo = json("{'rut':'19234587-1','contrasena':'cliente1'}");

        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk());
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.mensaje").value("Demasiados intentos"));
    }

    @Test
    @DisplayName("contraseñas: primera, cambio, recuperación y restablecimiento")
    void contrasenas() throws Exception {
        when(servicio.crearContrasena(eq("17550982-8"), any())).thenReturn(CUENTA);
        when(servicio.cambiarContrasena(eq("19234587-1"), any())).thenReturn(CUENTA);
        when(servicio.recuperar(eq("19234587-1"), any())).thenReturn(CUENTA);

        mvc.perform(post("/api/usuarios/17550982-8/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'contrasena':'venta123'}")))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/usuarios/19234587-1/contrasena").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'actual':'cliente1','nueva':'cliente2'}")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/usuarios/19234587-1/recuperar").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'codigo':'ABCD-2345','nueva':'nueva456'}")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/usuarios/19234587-1/recuperar").contentType(MediaType.APPLICATION_JSON)
                        .content(json("{'codigo':'x','nueva':'nueva456'}")))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/usuarios/17550982-8/contrasena")).andExpect(status().isNoContent());
        verify(servicio).restablecerContrasena("17550982-8");
    }

    @Test
    @DisplayName("un JSON mal formado responde 400")
    void jsonInvalido() throws Exception {
        mvc.perform(post("/api/usuarios/login").contentType(MediaType.APPLICATION_JSON).content("{x"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }
}
