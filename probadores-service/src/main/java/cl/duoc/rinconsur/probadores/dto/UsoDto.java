package cl.duoc.rinconsur.probadores.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Un uso de probador (se usa para crear, cerrar y listar).
public record UsoDto(
        @Schema(example = "1f0c6b8e-2c4f-4a8e-9a1e-3b7d2a1c9e55")
        @NotBlank(message = "es obligatorio") @Size(max = 40) String id,

        @Schema(example = "P-01")
        @NotBlank(message = "es obligatorio") @Size(max = 10) String codigoProbador,

        @Size(max = 40) String turnoId,

        @NotNull(message = "es obligatorio") Long inicio,

        Long termino,

        @Schema(example = "3")
        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        @Max(value = 10, message = "no puede ser más de 10")
        Integer prendasIngresadas,

        @Min(value = 0, message = "no puede ser negativo")
        Integer prendasDevueltas,

        @Schema(example = "16842311-K")
        @NotBlank(message = "es obligatorio") @Size(max = 12) String registroInicio,

        @Size(max = 12) String registroTermino,

        Boolean conAtraso
) {
}
