package cl.duoc.rinconsur.turnos.dto;

import java.time.LocalDate;

public record TurnoResponse(
        String id,
        String codigo,
        Integer numero,
        LocalDate fecha,
        Long horaIngreso,
        Boolean preferente,
        Integer prendas,
        String estado,
        Long horaLlamado,
        Long horaCierre,
        String motivoAbandono,
        String registradoPor,
        String clienteRut
) {
}
