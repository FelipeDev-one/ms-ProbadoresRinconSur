package cl.duoc.rinconsur.turnos.dto;

// Respuesta de GET /api/probadores/resumen en probadores-service.
public record ResumenProbadoresDto(long total, long habilitados, long libres) {
}
