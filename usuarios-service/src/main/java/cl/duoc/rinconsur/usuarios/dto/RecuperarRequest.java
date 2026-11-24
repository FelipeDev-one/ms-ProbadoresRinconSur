package cl.duoc.rinconsur.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecuperarRequest(
        @Schema(example = "K7QX-29MD", description = "Código de recuperación entregado al crear o cambiar la contraseña")
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9]{4}-?[A-Za-z0-9]{4}$", message = "debe tener el formato XXXX-XXXX")
        String codigo,

        @Schema(example = "nueva456")
        @NotBlank(message = "es obligatoria")
        @Size(min = 6, max = 64, message = "debe tener entre 6 y 64 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe tener al menos una letra y un número")
        String nueva
) {
}
