package cl.duoc.rinconsur.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Microservicio de usuarios: cuentas de clientes y colaboradores, ingreso y contraseñas.
@SpringBootApplication
public class UsuariosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsuariosServiceApplication.class, args);
    }
}
