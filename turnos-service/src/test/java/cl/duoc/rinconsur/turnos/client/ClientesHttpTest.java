package cl.duoc.rinconsur.turnos.client;

import cl.duoc.rinconsur.turnos.dto.ResumenProbadoresDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Prueba los clientes WebClient sin red: una ExchangeFunction simula la respuesta del otro servicio.
class ClientesHttpTest {

    private static WebClient.Builder responde(HttpStatus estado, String json) {
        ExchangeFunction intercambio = solicitud -> Mono.just(ClientResponse.create(estado)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build());
        return WebClient.builder().exchangeFunction(intercambio);
    }

    private static WebClient.Builder sinConexion() {
        ExchangeFunction intercambio = solicitud -> Mono.error(new WebClientRequestException(
                new java.net.ConnectException("rechazada"), HttpMethod.GET, URI.create("http://x"), HttpHeaders.EMPTY));
        return WebClient.builder().exchangeFunction(intercambio);
    }

    @Test
    @DisplayName("ProbadorClient lee el resumen de probadores-service")
    void resumen() {
        ProbadorClient cliente = new ProbadorClient(
                responde(HttpStatus.OK, "{\"total\":8,\"habilitados\":7,\"libres\":2}"), "http://probadores-service");

        assertEquals(new ResumenProbadoresDto(8, 7, 2), cliente.resumen());
    }

    @Test
    @DisplayName("si probadores-service no responde, 503")
    void resumenSinServicio() {
        ProbadorClient cliente = new ProbadorClient(sinConexion(), "http://probadores-service");

        ResponseStatusException e = assertThrows(ResponseStatusException.class, cliente::resumen);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatusCode());
    }

    @Test
    @DisplayName("UsuarioClient acepta solo cuentas de cliente activas")
    void esClienteActivo() {
        String cliente = "{\"rut\":\"19234587-1\",\"nombre\":\"Ana\",\"rol\":\"CLIENTE\",\"activo\":true}";
        String jefa = "{\"rut\":\"12984561-9\",\"nombre\":\"Patricia\",\"rol\":\"JEFATURA\",\"activo\":true}";

        assertTrue(new UsuarioClient(responde(HttpStatus.OK, cliente), "http://u").esClienteActivo("19234587-1"));
        assertFalse(new UsuarioClient(responde(HttpStatus.OK, jefa), "http://u").esClienteActivo("12984561-9"));
    }

    @Test
    @DisplayName("un RUT que no existe en usuarios-service no es cliente")
    void clienteNoExiste() {
        assertFalse(new UsuarioClient(responde(HttpStatus.NOT_FOUND, "{}"), "http://u").esClienteActivo("1-9"));
    }

    @Test
    @DisplayName("si usuarios-service no responde, 503")
    void usuariosSinServicio() {
        UsuarioClient cliente = new UsuarioClient(sinConexion(), "http://u");

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> cliente.esClienteActivo("1-9"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatusCode());
    }
}
