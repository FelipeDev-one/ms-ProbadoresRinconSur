package cl.duoc.rinconsur.turnos.model;

public enum EstadoTurno {
    EN_ESPERA,
    LLAMADO,
    ATENDIDO,
    ABANDONO;

    // En la fila: esperando o ya llamado al mesón.
    public boolean abierto() {
        return this == EN_ESPERA || this == LLAMADO;
    }
}
