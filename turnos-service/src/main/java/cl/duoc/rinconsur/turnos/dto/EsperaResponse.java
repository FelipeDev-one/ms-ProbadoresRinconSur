package cl.duoc.rinconsur.turnos.dto;

// Lo que ve el cliente al consultar su turno.
// minutosEstimados es null si el turno ya no está en la fila.
public record EsperaResponse(
        String codigo,
        String estado,
        int personasAntes,
        long probadoresLibres,
        Integer minutosEstimados
) {
}
