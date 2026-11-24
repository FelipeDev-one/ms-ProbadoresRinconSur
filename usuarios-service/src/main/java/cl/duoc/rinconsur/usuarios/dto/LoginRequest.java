package cl.duoc.rinconsur.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(example = "12.984.561-9")
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "^[0-9.]{7,10}-?[0-9kK]$", message = "debe tener el formato 12.345.678-9")
        String rut,

        @Schema(example = "clave123")
        @NotBlank(message = "es obligatoria")
        String contrasena
) {
}
