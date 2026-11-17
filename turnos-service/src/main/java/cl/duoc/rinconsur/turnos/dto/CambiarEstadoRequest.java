package cl.duoc.rinconsur.turnos.dto;

// estado: "LLAMADO", "ATENDIDO" o "ABANDONO".
public record CambiarEstadoRequest(String estado) {
}
