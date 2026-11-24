package cl.duoc.rinconsur.turnos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Mismos rangos que valida la pantalla de Parámetros de la app.
public record ParametrosDto(
        @Schema(example = "6") @NotNull(message = "es obligatorio") @Min(value = 1, message = "mínimo 1") @Max(value = 10, message = "máximo 10")
        Integer maxPrendas,
        @Schema(example = "8") @NotNull(message = "es obligatorio") @Min(value = 1, message = "mínimo 1") @Max(value = 60, message = "máximo 60")
        Integer tiempoReferenciaUso,
        @Schema(example = "1") @NotNull(message = "es obligatorio") @Min(value = 1, message = "mínimo 1") @Max(value = 30, message = "máximo 30")
        Integer minutosOrdenamiento,
        @Schema(example = "15") @NotNull(message = "es obligatorio") @Min(value = 1, message = "mínimo 1") @Max(value = 120, message = "máximo 120")
        Integer umbralProlongado,
        @Schema(example = "25") @NotNull(message = "es obligatorio") @Min(value = 1, message = "mínimo 1") @Max(value = 180, message = "máximo 180")
        Integer umbralAyuda,
        @Schema(example = "2") @NotNull(message = "es obligatorio") @Min(value = 1, message = "mínimo 1") @Max(value = 30, message = "máximo 30")
        Integer minutosPresentarse,
        @Schema(example = "27990") @NotNull(message = "es obligatorio") @Min(value = 0, message = "no puede ser negativo")
        Integer ticketPromedio,
        @Schema(example = "10") @NotNull(message = "es obligatorio") @Min(value = 0, message = "mínimo 0") @Max(value = 23, message = "máximo 23")
        Integer horaApertura,
        @Schema(example = "21") @NotNull(message = "es obligatorio") @Min(value = 1, message = "mínimo 1") @Max(value = 24, message = "máximo 24")
        Integer horaCierre,
        @Schema(example = "15") @NotNull(message = "es obligatorio") @Min(value = 0, message = "mínimo 0") @Max(value = 120, message = "máximo 120")
        Integer minutosCierreFila
) {
}
