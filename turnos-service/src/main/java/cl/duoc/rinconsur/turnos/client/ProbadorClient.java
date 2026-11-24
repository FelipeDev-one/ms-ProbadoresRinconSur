package cl.duoc.rinconsur.turnos.client;

import cl.duoc.rinconsur.turnos.dto.ResumenProbadoresDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.server.ResponseStatusException;

// turnos-service le pregunta a probadores-service cuántos probadores hay libres
// y habilitados para estimar la espera. No comparten tablas: solo HTTP.
@Component
public class ProbadorClient {

    private final WebClient webClient;

    public ProbadorClient(WebClient.Builder builder, @Value("${probadores.service.url}") String url) {
        this.webClient = builder.baseUrl(url).build();
    }

    public ResumenProbadoresDto resumen() {
        try {
            return webClient.get()
                    .uri("/api/probadores/resumen")
                    .retrieve()
                    .bodyToMono(ResumenProbadoresDto.class)
                    .block();
        } catch (WebClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de probadores no está disponible");
        }
    }
}
