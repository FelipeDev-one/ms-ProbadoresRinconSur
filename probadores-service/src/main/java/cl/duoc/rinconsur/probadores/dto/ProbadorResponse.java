package cl.duoc.rinconsur.probadores.dto;

public record ProbadorResponse(
        String codigo,
        String tipo,
        String estado,
        Long desde,
        String usoActivoId,
        String motivoFueraServicio,
        String fotoEvidencia
) {
}
