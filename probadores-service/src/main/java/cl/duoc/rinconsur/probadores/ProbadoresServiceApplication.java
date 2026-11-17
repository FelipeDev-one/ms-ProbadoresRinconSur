package cl.duoc.rinconsur.probadores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Microservicio de probadores: catálogo de probadores de la sección y su estado.
@SpringBootApplication
public class ProbadoresServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProbadoresServiceApplication.class, args);
    }
}
