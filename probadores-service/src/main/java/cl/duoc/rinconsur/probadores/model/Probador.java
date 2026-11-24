package cl.duoc.rinconsur.probadores.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "probadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Probador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "P-01", "P-02"…
    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoProbador tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProbador estado;

    // Desde cuándo está en este estado (epoch en milisegundos).
    @Column(nullable = false)
    private Long desde;

    // Uso en curso (si está ocupado).
    @Column(name = "uso_activo_id", length = 40)
    private String usoActivoId;

    @Column(name = "motivo_fuera_servicio", length = 120)
    private String motivoFueraServicio;

    // Ruta de la foto de evidencia en el teléfono que la tomó.
    @Column(name = "foto_evidencia", length = 300)
    private String fotoEvidencia;
}
