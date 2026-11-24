package cl.duoc.rinconsur.turnos.controller;

import cl.duoc.rinconsur.turnos.dto.CambiarEstadoRequest;
import cl.duoc.rinconsur.turnos.dto.CrearTurnoRequest;
import cl.duoc.rinconsur.turnos.dto.EsperaResponse;
import cl.duoc.rinconsur.turnos.dto.GuardarTurnoRequest;
import cl.duoc.rinconsur.turnos.dto.ParametrosDto;
import cl.duoc.rinconsur.turnos.dto.TurnoResponse;
import cl.duoc.rinconsur.turnos.service.TurnoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
@Tag(name = "Turnos", description = "Fila de espera de los probadores y parámetros de la sección")
public class TurnoController {

    private final TurnoService turnoService;

    @GetMapping
    @Operation(summary = "Lista los turnos", description = "Todos, o solo los de una fecha (yyyy-MM-dd).")
    public List<TurnoResponse> listar(@Parameter(example = "2026-11-24")
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return turnoService.listar(fecha);
    }

    @GetMapping("/fila")
    @Operation(summary = "Fila de hoy", description = "Turnos en espera o llamados; los preferentes primero.")
    public List<TurnoResponse> fila() {
        return turnoService.fila();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Toma un turno",
            description = "Con clienteRut lo toma el cliente desde su cuenta (se valida con usuarios-service); con registradoPor lo registra el personal.")
    @ApiResponse(responseCode = "201", description = "Turno creado")
    @ApiResponse(responseCode = "400", description = "Prendas fuera de rango o RUT con formato inválido")
    @ApiResponse(responseCode = "404", description = "El RUT no es de un cliente activo")
    @ApiResponse(responseCode = "409", description = "El cliente ya tiene un turno abierto")
    @ApiResponse(responseCode = "503", description = "usuarios-service no responde")
    public TurnoResponse crear(@Valid @RequestBody CrearTurnoRequest datos) {
        return turnoService.crear(datos);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Crea o actualiza un turno completo", description = "Lo usa la app al sincronizar.")
    @ApiResponse(responseCode = "400", description = "Datos no válidos")
    @ApiResponse(responseCode = "404", description = "El RUT no es de un cliente activo")
    @ApiResponse(responseCode = "409", description = "Número repetido en el día, cliente con otro turno o turno cerrado")
    public TurnoResponse guardar(@PathVariable String id, @Valid @RequestBody GuardarTurnoRequest datos) {
        return turnoService.guardar(id, datos);
    }

    @GetMapping("/cliente/{rut}")
    @Operation(summary = "Turno abierto de un cliente hoy")
    @ApiResponse(responseCode = "404", description = "No tiene turno en la fila")
    public TurnoResponse turnoDeCliente(@Parameter(example = "19234587-1") @PathVariable String rut) {
        return turnoService.turnoDeCliente(rut);
    }

    @GetMapping("/{numero}/espera")
    @Operation(summary = "Espera de un turno", description = "Personas antes y minutos estimados (consulta a probadores-service).")
    @ApiResponse(responseCode = "404", description = "No existe el turno hoy")
    @ApiResponse(responseCode = "503", description = "probadores-service no responde")
    public EsperaResponse espera(@Parameter(example = "1") @PathVariable int numero) {
        return turnoService.espera(numero);
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado de un turno",
            description = "LLAMADO, ATENDIDO, ABANDONO (con motivo) o NO_SE_PRESENTO. Un turno cerrado no se reabre.")
    @ApiResponse(responseCode = "400", description = "Estado no válido")
    @ApiResponse(responseCode = "404", description = "No existe el turno")
    @ApiResponse(responseCode = "409", description = "El turno ya estaba cerrado")
    public TurnoResponse cambiarEstado(@PathVariable String id, @Valid @RequestBody CambiarEstadoRequest datos) {
        return turnoService.cambiarEstado(id, datos);
    }

    @GetMapping("/parametros")
    @Operation(summary = "Parámetros de la sección", description = "Si nunca se guardaron, entrega los valores por defecto.")
    public ParametrosDto parametros() {
        return turnoService.parametros();
    }

    @PutMapping("/parametros")
    @Operation(summary = "Guarda los parámetros de la sección")
    @ApiResponse(responseCode = "400", description = "Valores fuera de rango")
    public ParametrosDto guardarParametros(@Valid @RequestBody ParametrosDto datos) {
        return turnoService.guardarParametros(datos);
    }
}
