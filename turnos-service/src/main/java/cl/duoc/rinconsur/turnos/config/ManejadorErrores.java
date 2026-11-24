package cl.duoc.rinconsur.turnos.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

// Respuestas de error con el mismo formato en todos los endpoints:
//   {"estado": 400, "mensaje": "...", "campos": {"prendas": "debe ser mayor o igual a 1"}}
@RestControllerAdvice
public class ManejadorErrores {

    // Falla una validación de @Valid (campo vacío, fuera de rango, formato…) → 400 con el detalle por campo.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", 400);
        cuerpo.put("mensaje", "Datos no válidos");
        cuerpo.put("campos", campos);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    // Errores de negocio lanzados por los servicios (404, 409, 401…).
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> negocio(ResponseStatusException e) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", e.getStatusCode().value());
        cuerpo.put("mensaje", e.getReason());
        return ResponseEntity.status(e.getStatusCode()).body(cuerpo);
    }

    // El cuerpo no es un JSON válido o un valor no corresponde al tipo esperado.
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> cuerpoInvalido(Exception e) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("estado", HttpStatus.BAD_REQUEST.value());
        cuerpo.put("mensaje", "El cuerpo de la solicitud no es válido");
        return ResponseEntity.badRequest().body(cuerpo);
    }
}
