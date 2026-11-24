package cl.duoc.rinconsur.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Punto de entrada único: la app móvil solo conoce esta dirección (puerto 8080).
// El gateway reenvía cada ruta al microservicio que corresponde, buscándolo en Eureka (lb://).
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
