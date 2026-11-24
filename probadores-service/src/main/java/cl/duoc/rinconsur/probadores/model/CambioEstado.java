package cl.duoc.rinconsur.probadores.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Historial: quién cambió el estado de qué probador y cuándo.
@Entity
@Table(name = "cambios_estado",
        uniqueConstraints = @UniqueConstraint(columnNames = {"codigo_probador", "fecha_hora", "nuevo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CambioEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_probador", nullable = false, length = 10)
    private String codigoProbador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProbador anterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProbador nuevo;

    @Column(name = "fecha_hora", nullable = false)
    private Long fechaHora;

    @Column(nullable = false, length = 12)
    private String colaborador;

    @Column(length = 120)
    private String motivo;
}
