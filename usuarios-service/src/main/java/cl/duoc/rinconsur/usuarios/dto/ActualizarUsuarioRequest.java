package cl.duoc.rinconsur.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Todos los campos son opcionales: solo se cambian los que vienen.
public record ActualizarUsuarioRequest(
        @Schema(example = "Ana María Pérez")
        @Size(max = 80, message = "puede tener hasta 80 caracteres")
        @Pattern(regexp = "^[\\p{L}' -]+ [\\p{L}' -]+$", message = "debe incluir nombre y apellido, solo letras")
        String nombre,

        @Schema(example = "ASISTENTE", description = "Solo para colaboradores")
        @Pattern(regexp = "(?i)ASISTENTE|VENDEDOR|JEFATURA", message = "debe ser ASISTENTE, VENDEDOR o JEFATURA")
        String rol,

        @Schema(example = "MANANA", allowableValues = {"MANANA", "TARDE", "FIN_DE_SEMANA"})
        @Pattern(regexp = "(?i)MANANA|TARDE|FIN_DE_SEMANA", message = "debe ser MANANA, TARDE o FIN_DE_SEMANA")
        String turnoLaboral,

        @Schema(example = "true")
        Boolean activo
) {
}
