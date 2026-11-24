package cl.duoc.rinconsur.turnos.client;

import cl.duoc.rinconsur.turnos.dto.UsuarioDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

// Antes de dar un turno a nombre de un cliente, se confirma con usuarios-service
// que esa cuenta existe, está activa y es de cliente.
@Component
public class UsuarioClient {

    private final WebClient webClient;

    public UsuarioClient(WebClient.Builder builder, @Value("${usuarios.service.url}") String url) {
        this.webClient = builder.baseUrl(url).build();
    }

    public boolean esClienteActivo(String rut) {
        try {
            UsuarioDto usuario = webClient.get()
                    .uri("/api/usuarios/{rut}", rut)
                    .retrieve()
                    .bodyToMono(UsuarioDto.class)
                    .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                    .block();
            return usuario != null && "CLIENTE".equals(usuario.rol()) && Boolean.TRUE.equals(usuario.activo());
        } catch (WebClientRequestException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de usuarios no está disponible");
        }
    }
}
