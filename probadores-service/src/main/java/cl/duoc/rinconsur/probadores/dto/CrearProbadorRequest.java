package cl.duoc.rinconsur.probadores.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

// El código lo asigna el servicio (primer P-xx libre). Si no se indica tipo, queda ESTANDAR.
public record CrearProbadorRequest(
        @Schema(example = "ACCESIBLE", allowableValues = {"ESTANDAR", "ACCESIBLE"})
        @Pattern(regexp = "(?i)ESTANDAR|ACCESIBLE", message = "debe ser ESTANDAR o ACCESIBLE")
        String tipo
) {
}
