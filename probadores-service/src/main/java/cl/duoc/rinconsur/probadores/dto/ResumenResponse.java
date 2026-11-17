package cl.duoc.rinconsur.probadores.dto;

// Lo que necesita turnos-service para estimar la espera.
// habilitados = todos menos los que están fuera de servicio.
public record ResumenResponse(long total, long habilitados, long libres) {
}
