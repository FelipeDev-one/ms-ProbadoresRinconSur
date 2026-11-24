package cl.duoc.rinconsur.turnos.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// @LoadBalanced: con una URL como http://probadores-service, WebClient le pregunta
// a Eureka dónde está ese servicio. Así no hay IP ni puerto fijos en el código.
@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
