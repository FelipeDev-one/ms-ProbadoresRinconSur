package cl.duoc.rinconsur.usuarios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration
public class SeguridadConfig {

    // BCrypt: hash con sal incluida. Dos usuarios con la misma contraseña quedan con hashes distintos.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // El servicio pide la hora a este reloj: así las pruebas pueden fijarla (bloqueo por intentos).
    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
