package cl.duoc.rinconsur.usuarios.model;

// CLIENTE crea su propia cuenta; los demás roles solo los crea la jefatura.
public enum Rol {
    CLIENTE,
    ASISTENTE,
    VENDEDOR,
    JEFATURA;

    public boolean esColaborador() {
        return this != CLIENTE;
    }
}
