package cl.duoc.rinconsur.turnos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarEstadoRequest(
        @Schema(example = "LLAMADO", allowableValues = {"LLAMADO", "ATENDIDO", "ABANDONO", "NO_SE_PRESENTO"})
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "(?i)LLAMADO|ATENDIDO|ABANDONO|NO_SE_PRESENTO",
                message = "debe ser LLAMADO, ATENDIDO, ABANDONO o NO_SE_PRESENTO")
        String estado,

        @Schema(example = "ESPERA_LARGA", description = "Solo para ABANDONO")
        @Pattern(regexp = "(?i)ESPERA_LARGA|SE_HIZO_TARDE|COMPRA_SIN_PROBARSE|OTRO", message = "motivo no válido")
        String motivo
) {
}
