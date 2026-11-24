package cl.duoc.rinconsur.probadores.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfiguracionTest {

    @Test
    @DisplayName("las configuraciones crean sus beans")
    void configuraciones() {
        assertEquals("probadores-service", new OpenApiConfig().documentacion().getInfo().getTitle());
        assertNotNull(new RelojConfig().reloj());
    }
}
