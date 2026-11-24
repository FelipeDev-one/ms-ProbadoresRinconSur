package cl.duoc.rinconsur.usuarios.config;

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
                .title("usuarios-service")
                .version("1.0.0")
                .description("Cuentas de clientes y colaboradores: registro de clientes, ingreso con RUT y contraseña, perfil y cambio de contraseña."));
    }
}
