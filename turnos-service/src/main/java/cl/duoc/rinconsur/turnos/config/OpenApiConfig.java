package cl.duoc.rinconsur.turnos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Datos que muestra Swagger en el encabezado de la documentación.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI documentacion() {
        return new OpenAPI().info(new Info()
                .title("turnos-service")
                .version("1.0.0")
                .description("Fila de espera de los probadores: tomar turno, consultar la espera y cambiar el estado del turno."));
    }
}
