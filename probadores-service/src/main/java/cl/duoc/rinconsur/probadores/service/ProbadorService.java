package cl.duoc.rinconsur.probadores.service;

import cl.duoc.rinconsur.probadores.dto.CambiarEstadoRequest;
import cl.duoc.rinconsur.probadores.dto.CrearProbadorRequest;
import cl.duoc.rinconsur.probadores.dto.ProbadorResponse;
import cl.duoc.rinconsur.probadores.dto.ResumenResponse;
import cl.duoc.rinconsur.probadores.model.EstadoProbador;
import cl.duoc.rinconsur.probadores.model.Probador;
import cl.duoc.rinconsur.probadores.model.TipoProbador;
import cl.duoc.rinconsur.probadores.repository.ProbadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProbadorService {

    public static final int MAX_PROBADORES = 30;

    private final ProbadorRepository probadorRepository;

    // estado es opcional: "LIBRE", "OCUPADO"… filtra la lista.
    public List<ProbadorResponse> listar(String estado) {
        List<Probador> probadores = (estado == null || estado.isBlank())
                ? probadorRepository.findAllByOrderByCodigoAsc()
                : probadorRepository.findByEstadoOrderByCodigoAsc(aEstado(estado));
        return probadores.stream().map(this::aRespuesta).toList();
    }

    public ProbadorResponse obtener(String codigo) {
        return aRespuesta(buscar(codigo));
    }

    public ResumenResponse resumen() {
        long total = probadorRepository.count();
        long fuera = probadorRepository.countByEstado(EstadoProbador.FUERA_DE_SERVICIO);
        long libres = probadorRepository.countByEstado(EstadoProbador.LIBRE);
        return new ResumenResponse(total, total - fuera, libres);
    }

    // Agrega un probador libre con el primer código que no esté usado (P-01, P-02…).
    public ProbadorResponse crear(CrearProbadorRequest datos) {
        TipoProbador tipo = aTipo(datos.tipo());
        List<Probador> existentes = probadorRepository.findAll();
        if (existentes.size() >= MAX_PROBADORES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El máximo es " + MAX_PROBADORES + " probadores");
        }
        Set<String> usados = existentes.stream().map(Probador::getCodigo).collect(Collectors.toSet());
        int numero = 1;
        while (usados.contains(codigo(numero))) {
            numero++;
        }
        Probador nuevo = Probador.builder()
                .codigo(codigo(numero))
                .tipo(tipo)
                .estado(EstadoProbador.LIBRE)
                .build();
        return aRespuesta(probadorRepository.save(nuevo));
    }

    // Solo se aceptan los cambios de estado válidos (por ejemplo, LIBRE no pasa directo a EN_ORDENAMIENTO).
    public ProbadorResponse cambiarEstado(String codigo, CambiarEstadoRequest datos) {
        Probador probador = buscar(codigo);
        EstadoProbador nuevo = aEstado(datos.estado());
        if (!probador.getEstado().puedePasarA(nuevo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    codigo + " no puede pasar de " + probador.getEstado() + " a " + nuevo);
        }
        probador.setEstado(nuevo);
        return aRespuesta(probadorRepository.save(probador));
    }

    public static String codigo(int numero) {
        return String.format("P-%02d", numero);
    }

    private Probador buscar(String codigo) {
        return probadorRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el probador " + codigo));
    }

    private EstadoProbador aEstado(String texto) {
        try {
            return EstadoProbador.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido: " + texto);
        }
    }

    private TipoProbador aTipo(String texto) {
        if (texto == null || texto.isBlank()) {
            return TipoProbador.ESTANDAR;
        }
        try {
            return TipoProbador.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo no válido: " + texto);
        }
    }

    private ProbadorResponse aRespuesta(Probador p) {
        return new ProbadorResponse(p.getId(), p.getCodigo(), p.getTipo().name(), p.getEstado().name());
    }
}
