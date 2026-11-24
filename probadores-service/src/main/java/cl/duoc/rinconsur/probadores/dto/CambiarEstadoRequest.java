package cl.duoc.rinconsur.probadores.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarEstadoRequest(
        @Schema(example = "OCUPADO", allowableValues = {"LIBRE", "OCUPADO", "EN_ORDENAMIENTO", "FUERA_DE_SERVICIO"})
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "(?i)LIBRE|OCUPADO|EN_ORDENAMIENTO|FUERA_DE_SERVICIO",
                message = "debe ser LIBRE, OCUPADO, EN_ORDENAMIENTO o FUERA_DE_SERVICIO")
        String estado
) {
}
