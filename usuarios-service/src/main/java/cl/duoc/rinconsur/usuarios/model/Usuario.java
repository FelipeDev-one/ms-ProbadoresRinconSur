package cl.duoc.rinconsur.usuarios.model;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Normalizado: sin puntos, con guion y K mayúscula (12984561-9).
    @Column(nullable = false, unique = true, length = 12)
    private String rut;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    // Solo colaboradores.
    @Enumerated(EnumType.STRING)
    @Column(name = "turno_laboral", length = 20)
    private TurnoLaboral turnoLaboral;

    @Column(nullable = false)
    private Boolean activo;

    @Column(nullable = false)
    private LocalDateTime creado;

    // Hash BCrypt. null = el colaborador aún no elige su contraseña (la crea en su primer ingreso).
    @Column(name = "contrasena_hash", length = 100)
    private String contrasenaHash;

    // Hash BCrypt del código de recuperación (se entrega una sola vez al crear o cambiar la contraseña).
    @Column(name = "recuperacion_hash", length = 100)
    private String recuperacionHash;

    @Builder.Default
    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    // Momento (epoch en milisegundos) hasta el que la cuenta queda bloqueada por intentos fallidos.
    @Builder.Default
    @Column(name = "bloqueada_hasta", nullable = false)
    private Long bloqueadaHasta = 0L;
}
