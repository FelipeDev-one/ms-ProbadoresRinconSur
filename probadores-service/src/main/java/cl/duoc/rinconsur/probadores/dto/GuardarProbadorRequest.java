package cl.duoc.rinconsur.probadores.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// Estado completo de un probador, tal como lo maneja la app (crear o actualizar).
public record GuardarProbadorRequest(
        @Schema(example = "ESTANDAR", allowableValues = {"ESTANDAR", "ACCESIBLE"})
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "(?i)ESTANDAR|ACCESIBLE", message = "debe ser ESTANDAR o ACCESIBLE")
        String tipo,

        @Schema(example = "LIBRE", allowableValues = {"LIBRE", "OCUPADO", "EN_ORDENAMIENTO", "FUERA_DE_SERVICIO"})
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "(?i)LIBRE|OCUPADO|EN_ORDENAMIENTO|FUERA_DE_SERVICIO",
                message = "debe ser LIBRE, OCUPADO, EN_ORDENAMIENTO o FUERA_DE_SERVICIO")
        String estado,

        @Schema(example = "1795000000000", description = "Desde cuándo está en este estado (epoch en milisegundos)")
        @NotNull(message = "es obligatorio")
        @PositiveOrZero(message = "no puede ser negativo")
        Long desde,

        @Size(max = 40, message = "puede tener hasta 40 caracteres")
        String usoActivoId,

        @Schema(example = "Cortina rota")
        @Size(max = 120, message = "puede tener hasta 120 caracteres")
        String motivoFueraServicio,

        @Size(max = 300, message = "puede tener hasta 300 caracteres")
        String fotoEvidencia
) {
}
