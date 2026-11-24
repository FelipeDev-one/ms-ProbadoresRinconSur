package cl.duoc.rinconsur.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// Registro de servicios: cada microservicio se anota aquí al arrancar y el
// API Gateway (y turnos-service) los encuentran por su nombre, sin IP fija.
// Panel: http://localhost:8761
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
