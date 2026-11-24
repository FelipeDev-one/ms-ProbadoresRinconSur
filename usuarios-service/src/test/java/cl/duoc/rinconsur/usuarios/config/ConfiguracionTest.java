package cl.duoc.rinconsur.usuarios.config;

import cl.duoc.rinconsur.usuarios.model.Rol;
import cl.duoc.rinconsur.usuarios.util.CodigoRecuperacion;
import cl.duoc.rinconsur.usuarios.util.RutValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionTest {

    @Test
    @DisplayName("RUT: normaliza y valida el dígito verificador (incluida la K)")
    void rut() {
        assertEquals("12984561-9", RutValidator.normalizar("12.984.561-9"));
        assertEquals("1", RutValidator.normalizar("1"));
        assertTrue(RutValidator.esValido("12.984.561-9"));
        assertTrue(RutValidator.esValido("16842311-k"));
        assertFalse(RutValidator.esValido("12.984.561-0"));
        assertFalse(RutValidator.esValido("123-4"));
        assertFalse(RutValidator.esValido("abc"));
        assertFalse(RutValidator.esValido("12a45678-9"));
        assertFalse(RutValidator.esValido(null));
    }

    @Test
    @DisplayName("código de recuperación: formato XXXX-XXXX y normalización")
    void codigo() {
        assertTrue(CodigoRecuperacion.generar().matches("[A-Z2-9]{4}-[A-Z2-9]{4}"));
        assertEquals("ABCD2345", CodigoRecuperacion.normalizar("abcd-2345"));
    }

    @Test
    @DisplayName("configuraciones y roles")
    void varios() {
        assertEquals("usuarios-service", new OpenApiConfig().documentacion().getInfo().getTitle());
        assertNotNull(new SeguridadConfig().passwordEncoder());
        assertNotNull(new SeguridadConfig().reloj());
        assertFalse(Rol.CLIENTE.esColaborador());
        assertTrue(Rol.VENDEDOR.esColaborador());
    }
}
