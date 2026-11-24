package cl.duoc.rinconsur.turnos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

// El id es un UUID: lo genera la app (o este servicio si no viene) para que ambos lados lo reconozcan.
@Entity
@Table(name = "turnos", uniqueConstraints = @UniqueConstraint(columnNames = {"fecha", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turno {

    @Id
    @Column(length = 40)
    private String id;

    // Número del día: vuelve a partir en 1 cada día (T-001, T-002…).
    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false)
    private LocalDate fecha;

    // Horas en epoch milisegundos, igual que en la app.
    @Column(name = "hora_ingreso", nullable = false)
    private Long horaIngreso;

    @Column(nullable = false)
    private Boolean preferente;

    @Column(nullable = false)
    private Integer prendas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTurno estado;

    @Column(name = "hora_llamado")
    private Long horaLlamado;

    @Column(name = "hora_cierre")
    private Long horaCierre;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_abandono", length = 30)
    private MotivoAbandono motivoAbandono;

    // RUT del colaborador que lo registró en el mesón; null si lo tomó el propio cliente.
    @Column(name = "registrado_por", length = 12)
    private String registradoPor;

    // RUT del cliente si tomó el turno desde su cuenta.
    @Column(name = "cliente_rut", length = 12)
    private String clienteRut;

    public String getCodigo() {
        return String.format("T-%03d", numero);
    }
}
