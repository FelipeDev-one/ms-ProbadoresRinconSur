package cl.duoc.rinconsur.probadores.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un uso de probador: desde que el cliente entra hasta que devuelve las prendas.
// El id lo genera la app (UUID) para poder identificarlo aunque se cree sin conexión al servidor.
@Entity
@Table(name = "usos", indexes = @Index(columnList = "codigo_probador"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Uso {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "codigo_probador", nullable = false, length = 10)
    private String codigoProbador;

    // Turno de la fila que se atendió (null si entró sin fila). Pertenece a turnos-service.
    @Column(name = "turno_id", length = 40)
    private String turnoId;

    @Column(nullable = false)
    private Long inicio;

    private Long termino;

    @Column(name = "prendas_ingresadas", nullable = false)
    private Integer prendasIngresadas;

    @Column(name = "prendas_devueltas")
    private Integer prendasDevueltas;

    @Column(name = "registro_inicio", nullable = false, length = 12)
    private String registroInicio;

    @Column(name = "registro_termino", length = 12)
    private String registroTermino;

    @Column(name = "con_atraso", nullable = false)
    private Boolean conAtraso;
}
