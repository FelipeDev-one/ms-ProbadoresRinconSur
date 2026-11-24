package cl.duoc.rinconsur.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// La jefatura crea la cuenta sin contraseña: el colaborador la elige en su primer ingreso.
public record CrearColaboradorRequest(
        @Schema(example = "17.550.982-8")
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "^[0-9.]{7,10}-?[0-9kK]$", message = "debe tener el formato 12.345.678-9")
        String rut,

        @Schema(example = "Javiera Muñoz")
        @NotBlank(message = "es obligatorio")
        @Size(max = 80, message = "puede tener hasta 80 caracteres")
        @Pattern(regexp = "^[\\p{L}' -]+ [\\p{L}' -]+$", message = "debe incluir nombre y apellido, solo letras")
        String nombre,

        @Schema(example = "VENDEDOR", allowableValues = {"ASISTENTE", "VENDEDOR", "JEFATURA"})
        @NotBlank(message = "es obligatorio")
        @Pattern(regexp = "(?i)ASISTENTE|VENDEDOR|JEFATURA", message = "debe ser ASISTENTE, VENDEDOR o JEFATURA")
        String rol,

        @Schema(example = "MANANA", allowableValues = {"MANANA", "TARDE", "FIN_DE_SEMANA"})
        @Pattern(regexp = "(?i)MANANA|TARDE|FIN_DE_SEMANA", message = "debe ser MANANA, TARDE o FIN_DE_SEMANA")
        @NotBlank(message = "es obligatorio")
        String turnoLaboral
) {
}
