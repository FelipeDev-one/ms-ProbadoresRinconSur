package cl.duoc.rinconsur.turnos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CrearTurnoRequest(
        @Schema(example = "3", description = "Prendas que va a probarse (RN-02: 1 al máximo de los parámetros)")
        @NotNull(message = "es obligatorio")
        @Min(value = 1, message = "debe ser al menos 1")
        Integer prendas,

        @Schema(example = "false", description = "Atención preferente")
        Boolean preferente,

        @Schema(example = "19234587-1", description = "RUT del cliente si toma el turno desde su cuenta")
        @Pattern(regexp = "^$|^\\d{7,8}-[\\dkK]$", message = "debe tener el formato 12345678-9")
        String clienteRut,

        @Schema(example = "16842311-K", description = "RUT del colaborador si lo registra en el mesón")
        @Pattern(regexp = "^$|^\\d{7,8}-[\\dkK]$", message = "debe tener el formato 12345678-9")
        String registradoPor
) {
}
