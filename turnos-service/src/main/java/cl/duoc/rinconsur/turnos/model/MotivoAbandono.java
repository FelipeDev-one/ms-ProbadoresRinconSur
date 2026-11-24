package cl.duoc.rinconsur.turnos.model;

// Mismos motivos que la app. NO_INFORMADO: el turno llamado no se presentó.
// CIERRE_TIENDA: turnos que quedaron abiertos al cerrar la jornada.
public enum MotivoAbandono {
    ESPERA_LARGA,
    SE_HIZO_TARDE,
    COMPRA_SIN_PROBARSE,
    OTRO,
    NO_INFORMADO,
    CIERRE_TIENDA
}
