package cl.duoc.rinconsur.probadores.controller;

import cl.duoc.rinconsur.probadores.dto.CambiarEstadoRequest;
import cl.duoc.rinconsur.probadores.dto.CrearProbadorRequest;
import cl.duoc.rinconsur.probadores.dto.ProbadorResponse;
import cl.duoc.rinconsur.probadores.dto.ResumenResponse;
import cl.duoc.rinconsur.probadores.service.ProbadorService;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping("/api/probadores")
@RequiredArgsConstructor
public class ProbadorController {

    private final ProbadorService probadorService;

    @GetMapping
    public List<ProbadorResponse> listar(@RequestParam(required = false) String estado) {
        return probadorService.listar(estado);
    }

    @GetMapping("/resumen")
    public ResumenResponse resumen() {
        return probadorService.resumen();
    }

    @GetMapping("/{codigo}")
    public ProbadorResponse obtener(@PathVariable String codigo) {
        return probadorService.obtener(codigo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProbadorResponse crear(@RequestBody CrearProbadorRequest datos) {
        return probadorService.crear(datos);
    }

    @PutMapping("/{codigo}/estado")
    public ProbadorResponse cambiarEstado(@PathVariable String codigo, @RequestBody CambiarEstadoRequest datos) {
        return probadorService.cambiarEstado(codigo, datos);
    }
}
