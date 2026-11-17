package cl.duoc.rinconsur.probadores.model;

// Mismos estados y transiciones que la app (EstadoProbador.puedePasarA).
public enum EstadoProbador {
    LIBRE,
    OCUPADO,
    EN_ORDENAMIENTO,
    FUERA_DE_SERVICIO;

    public boolean puedePasarA(EstadoProbador nuevo) {
        return switch (this) {
            case LIBRE -> nuevo == OCUPADO || nuevo == FUERA_DE_SERVICIO;
            case OCUPADO -> nuevo == EN_ORDENAMIENTO || nuevo == FUERA_DE_SERVICIO;
            case EN_ORDENAMIENTO -> nuevo == LIBRE || nuevo == FUERA_DE_SERVICIO;
            case FUERA_DE_SERVICIO -> nuevo == LIBRE;
        };
    }
}
