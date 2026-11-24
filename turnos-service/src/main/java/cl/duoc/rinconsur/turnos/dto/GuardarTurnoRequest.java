package cl.duoc.rinconsur.turnos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Estado completo de un turno tal como lo maneja la app (lo envía al sincronizar).
public record GuardarTurnoRequest(
        @Schema(example = "5") @NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser al menos 1")
        Integer numero,

        @Schema(example = "2026-11-24") @NotNull(message = "es obligatorio")
        LocalDate fecha,

        @NotNull(message = "es obligatorio")
        Long horaIngreso,

        Boolean preferente,

        @Schema(example = "3") @NotNull(message = "es obligatorio") @Min(value = 1, message = "debe ser al menos 1")
        Integer prendas,

        @Schema(example = "EN_ESPERA")
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "EN_ESPERA|LLAMADO|ATENDIDO|ABANDONO|NO_SE_PRESENTO", message = "estado no válido")
        String estado,

        Long horaLlamado,

        Long horaCierre,

        @Pattern(regexp = "ESPERA_LARGA|SE_HIZO_TARDE|COMPRA_SIN_PROBARSE|OTRO|NO_INFORMADO|CIERRE_TIENDA",
                message = "motivo no válido")
        String motivoAbandono,

        @Size(max = 12) String registradoPor,

        @Size(max = 12) String clienteRut
) {
}
