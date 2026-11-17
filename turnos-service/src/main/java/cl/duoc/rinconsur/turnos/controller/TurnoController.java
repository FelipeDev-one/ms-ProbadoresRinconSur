package cl.duoc.rinconsur.turnos.controller;

import cl.duoc.rinconsur.turnos.dto.CambiarEstadoRequest;
import cl.duoc.rinconsur.turnos.dto.CrearTurnoRequest;
import cl.duoc.rinconsur.turnos.dto.EsperaResponse;
import cl.duoc.rinconsur.turnos.dto.TurnoResponse;
import cl.duoc.rinconsur.turnos.service.TurnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @GetMapping("/fila")
    public List<TurnoResponse> fila() {
        return turnoService.fila();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoResponse crear(@RequestBody CrearTurnoRequest datos) {
        return turnoService.crear(datos);
    }

    @GetMapping("/{numero}/espera")
    public EsperaResponse espera(@PathVariable int numero) {
        return turnoService.espera(numero);
    }

    @PutMapping("/{id}/estado")
    public TurnoResponse cambiarEstado(@PathVariable Long id, @RequestBody CambiarEstadoRequest datos) {
        return turnoService.cambiarEstado(id, datos);
    }
}
