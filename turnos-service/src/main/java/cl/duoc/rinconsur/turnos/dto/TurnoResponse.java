package cl.duoc.rinconsur.turnos.dto;

import java.time.LocalDateTime;

public record TurnoResponse(
        Long id,
        String codigo,
        Integer numero,
        Integer prendas,
        Boolean preferente,
        String estado,
        LocalDateTime horaIngreso,
        String clienteRut
) {
}
