package cl.duoc.rinconsur.turnos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Microservicio de turnos: la fila de espera de los probadores y el tiempo estimado.
@SpringBootApplication
public class TurnosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TurnosServiceApplication.class, args);
    }
}
