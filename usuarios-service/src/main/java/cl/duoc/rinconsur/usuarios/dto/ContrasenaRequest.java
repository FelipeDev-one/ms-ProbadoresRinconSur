package cl.duoc.rinconsur.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Primera contraseña de un colaborador creado por la jefatura.
public record ContrasenaRequest(
        @Schema(example = "venta123")
        @NotBlank(message = "es obligatoria")
        @Size(min = 6, max = 64, message = "debe tener entre 6 y 64 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe tener al menos una letra y un número")
        String contrasena
) {
}
