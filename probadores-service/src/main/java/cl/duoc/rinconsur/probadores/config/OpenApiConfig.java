package cl.duoc.rinconsur.probadores.config;

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
                .title("probadores-service")
                .version("1.0.0")
                .description("Probadores de la sección Vestuario: listado, filtro por estado, resumen y cambios de estado."));
    }
}
