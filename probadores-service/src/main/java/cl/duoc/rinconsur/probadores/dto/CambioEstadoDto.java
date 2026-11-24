package cl.duoc.rinconsur.probadores.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CambioEstadoDto(
        @Schema(example = "P-01") @NotBlank(message = "es obligatorio") @Size(max = 10) String codigoProbador,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "LIBRE|OCUPADO|EN_ORDENAMIENTO|FUERA_DE_SERVICIO", message = "estado no válido")
        String anterior,

        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "LIBRE|OCUPADO|EN_ORDENAMIENTO|FUERA_DE_SERVICIO", message = "estado no válido")
        String nuevo,

        @NotNull(message = "es obligatorio") Long fechaHora,

        @Schema(example = "16842311-K") @NotBlank(message = "es obligatorio") @Size(max = 12) String colaborador,

        @Size(max = 120) String motivo
) {
}
