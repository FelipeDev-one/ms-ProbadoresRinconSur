package cl.duoc.rinconsur.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Primera jefatura: solo se puede crear cuando el sistema aún no tiene ninguna.
public record ConfiguracionInicialRequest(
        @Schema(example = "12.984.561-9")
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "^[0-9.]{7,10}-?[0-9kK]$", message = "debe tener el formato 12.345.678-9")
        String rut,

        @Schema(example = "Patricia Morales")
        @NotBlank(message = "es obligatorio")
        @Size(max = 80, message = "puede tener hasta 80 caracteres")
        @Pattern(regexp = "^[\\p{L}' -]+ [\\p{L}' -]+$", message = "debe incluir nombre y apellido, solo letras")
        String nombre,

        @Schema(example = "MANANA", allowableValues = {"MANANA", "TARDE", "FIN_DE_SEMANA"})
        @Pattern(regexp = "(?i)MANANA|TARDE|FIN_DE_SEMANA", message = "debe ser MANANA, TARDE o FIN_DE_SEMANA")
        @NotBlank(message = "es obligatorio")
        String turnoLaboral,

        @Schema(example = "clave123")
        @NotBlank(message = "es obligatoria")
        @Size(min = 6, max = 64, message = "debe tener entre 6 y 64 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe tener al menos una letra y un número")
        String contrasena
) {
}
