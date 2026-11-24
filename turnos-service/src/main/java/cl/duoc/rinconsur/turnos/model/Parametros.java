package cl.duoc.rinconsur.turnos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Parámetros de la sección (una sola fila, id = 1). Los valores por defecto son los de la app.
@Entity
@Table(name = "parametros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parametros {

    public static final int ID_UNICO = 1;

    @Id
    private Integer id;

    @Column(name = "max_prendas", nullable = false)
    private Integer maxPrendas;

    @Column(name = "tiempo_referencia_uso", nullable = false)
    private Integer tiempoReferenciaUso;

    @Column(name = "minutos_ordenamiento", nullable = false)
    private Integer minutosOrdenamiento;

    @Column(name = "umbral_prolongado", nullable = false)
    private Integer umbralProlongado;

    @Column(name = "umbral_ayuda", nullable = false)
    private Integer umbralAyuda;

    @Column(name = "minutos_presentarse", nullable = false)
    private Integer minutosPresentarse;

    @Column(name = "ticket_promedio", nullable = false)
    private Integer ticketPromedio;

    @Column(name = "hora_apertura", nullable = false)
    private Integer horaApertura;

    @Column(name = "hora_cierre", nullable = false)
    private Integer horaCierre;

    @Column(name = "minutos_cierre_fila", nullable = false)
    private Integer minutosCierreFila;

    public static Parametros porDefecto() {
        return new Parametros(ID_UNICO, 6, 8, 1, 15, 25, 2, 27990, 10, 21, 15);
    }
}
