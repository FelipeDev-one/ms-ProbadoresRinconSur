package cl.duoc.rinconsur.probadores.controller;

import cl.duoc.rinconsur.probadores.dto.CambiarEstadoRequest;
import cl.duoc.rinconsur.probadores.dto.CambioEstadoDto;
import cl.duoc.rinconsur.probadores.dto.CrearProbadorRequest;
import cl.duoc.rinconsur.probadores.dto.GuardarProbadorRequest;
import cl.duoc.rinconsur.probadores.dto.ProbadorResponse;
import cl.duoc.rinconsur.probadores.dto.ResumenResponse;
import cl.duoc.rinconsur.probadores.dto.UsoDto;
import cl.duoc.rinconsur.probadores.service.ProbadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/probadores")
@RequiredArgsConstructor
@Tag(name = "Probadores", description = "Probadores de la sección, sus usos y el historial de estados")
public class ProbadorController {

    private final ProbadorService probadorService;

    @GetMapping
    @Operation(summary = "Lista los probadores", description = "El filtro por estado es opcional.")
    @ApiResponse(responseCode = "400", description = "Estado no válido")
    public List<ProbadorResponse> listar(@Parameter(example = "LIBRE") @RequestParam(required = false) String estado) {
        return probadorService.listar(estado);
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen de la sección", description = "Total, habilitados (no fuera de servicio) y libres. Lo usa turnos-service.")
    public ResumenResponse resumen() {
        return probadorService.resumen();
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Obtiene un probador por su código")
    @ApiResponse(responseCode = "404", description = "No existe")
    public ProbadorResponse obtener(@Parameter(example = "P-01") @PathVariable String codigo) {
        return probadorService.obtener(codigo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega un probador con el primer código libre")
    @ApiResponse(responseCode = "409", description = "Ya hay 30 probadores")
    public ProbadorResponse crear(@Valid @RequestBody CrearProbadorRequest datos) {
        return probadorService.crear(datos);
    }

    @PutMapping("/{codigo}")
    @Operation(summary = "Crea o actualiza un probador completo", description = "Lo usa la app al sincronizar. Valida la transición de estado.")
    @ApiResponse(responseCode = "400", description = "Datos no válidos")
    @ApiResponse(responseCode = "409", description = "Transición no permitida o máximo de probadores")
    public ProbadorResponse guardar(@Parameter(example = "P-01") @PathVariable String codigo,
                                    @Valid @RequestBody GuardarProbadorRequest datos) {
        return probadorService.guardar(codigo, datos);
    }

    @PutMapping("/{codigo}/estado")
    @Operation(summary = "Cambia el estado de un probador",
            description = "LIBRE → OCUPADO → EN_ORDENAMIENTO → LIBRE. Cualquiera puede pasar a FUERA_DE_SERVICIO, que solo vuelve a LIBRE.")
    @ApiResponse(responseCode = "409", description = "Transición no permitida")
    public ProbadorResponse cambiarEstado(@PathVariable String codigo, @Valid @RequestBody CambiarEstadoRequest datos) {
        return probadorService.cambiarEstado(codigo, datos);
    }

    @DeleteMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un probador", description = "Solo si nunca se usó y no está en uso.")
    @ApiResponse(responseCode = "404", description = "No existe")
    @ApiResponse(responseCode = "409", description = "Tiene usos, está en uso o es el último")
    public void eliminar(@PathVariable String codigo) {
        probadorService.eliminar(codigo);
    }

    @GetMapping("/usos")
    @Operation(summary = "Lista todos los usos de probadores")
    public List<UsoDto> usos() {
        return probadorService.usos();
    }

    @PutMapping("/usos/{id}")
    @Operation(summary = "Crea o cierra un uso", description = "Al cerrarlo se informan el término y las prendas devueltas.")
    @ApiResponse(responseCode = "400", description = "Datos no válidos (p. ej. devuelve más prendas de las que entraron)")
    @ApiResponse(responseCode = "404", description = "No existe el probador")
    public UsoDto guardarUso(@PathVariable String id, @Valid @RequestBody UsoDto datos) {
        return probadorService.guardarUso(id, datos);
    }

    @GetMapping("/cambios")
    @Operation(summary = "Historial de cambios de estado")
    public List<CambioEstadoDto> cambios() {
        return probadorService.cambios();
    }

    @PostMapping("/cambios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un cambio de estado en el historial", description = "Si ya existe, no se duplica.")
    @ApiResponse(responseCode = "404", description = "No existe el probador")
    public CambioEstadoDto registrarCambio(@Valid @RequestBody CambioEstadoDto datos) {
        return probadorService.registrarCambio(datos);
    }
}
