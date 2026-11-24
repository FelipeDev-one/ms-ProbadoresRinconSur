package cl.duoc.rinconsur.probadores.service;

import cl.duoc.rinconsur.probadores.dto.CambiarEstadoRequest;
import cl.duoc.rinconsur.probadores.dto.CambioEstadoDto;
import cl.duoc.rinconsur.probadores.dto.CrearProbadorRequest;
import cl.duoc.rinconsur.probadores.dto.GuardarProbadorRequest;
import cl.duoc.rinconsur.probadores.dto.ProbadorResponse;
import cl.duoc.rinconsur.probadores.dto.ResumenResponse;
import cl.duoc.rinconsur.probadores.dto.UsoDto;
import cl.duoc.rinconsur.probadores.model.CambioEstado;
import cl.duoc.rinconsur.probadores.model.EstadoProbador;
import cl.duoc.rinconsur.probadores.model.Probador;
import cl.duoc.rinconsur.probadores.model.TipoProbador;
import cl.duoc.rinconsur.probadores.model.Uso;
import cl.duoc.rinconsur.probadores.repository.CambioEstadoRepository;
import cl.duoc.rinconsur.probadores.repository.ProbadorRepository;
import cl.duoc.rinconsur.probadores.repository.UsoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProbadorService {

    public static final int MAX_PROBADORES = 30;

    private final ProbadorRepository probadorRepository;
    private final UsoRepository usoRepository;
    private final CambioEstadoRepository cambioRepository;
    private final Clock reloj;

    public ProbadorService(ProbadorRepository probadorRepository, UsoRepository usoRepository,
                           CambioEstadoRepository cambioRepository, Clock reloj) {
        this.probadorRepository = probadorRepository;
        this.usoRepository = usoRepository;
        this.cambioRepository = cambioRepository;
        this.reloj = reloj;
    }

    // ------------------------------ PROBADORES ------------------------------

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
        TipoProbador tipo = aTipo(datos.tipo() == null || datos.tipo().isBlank() ? "ESTANDAR" : datos.tipo());
        List<Probador> existentes = probadorRepository.findAll();
        if (existentes.size() >= MAX_PROBADORES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El máximo es " + MAX_PROBADORES + " probadores");
        }
        Set<String> usados = existentes.stream().map(Probador::getCodigo).collect(Collectors.toSet());
        int numero = 1;
        while (usados.contains(codigo(numero))) {
            numero++;
        }
        return aRespuesta(probadorRepository.save(Probador.builder()
                .codigo(codigo(numero)).tipo(tipo).estado(EstadoProbador.LIBRE).desde(reloj.millis()).build()));
    }

    // Crea o actualiza el estado completo de un probador (lo usa la app al sincronizar).
    // Si cambia de estado, debe ser una transición permitida (RN de la app).
    public ProbadorResponse guardar(String codigo, GuardarProbadorRequest datos) {
        if (!codigo.matches("P-\\d{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código debe tener el formato P-01");
        }
        EstadoProbador nuevo = aEstado(datos.estado());
        Probador probador = probadorRepository.findByCodigo(codigo).orElse(null);
        if (probador == null) {
            if (probadorRepository.count() >= MAX_PROBADORES) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El máximo es " + MAX_PROBADORES + " probadores");
            }
            probador = Probador.builder().codigo(codigo).build();
        } else if (probador.getEstado() != nuevo && !probador.getEstado().puedePasarA(nuevo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    codigo + " no puede pasar de " + probador.getEstado() + " a " + nuevo);
        }
        probador.setTipo(aTipo(datos.tipo()));
        probador.setEstado(nuevo);
        probador.setDesde(datos.desde());
        probador.setUsoActivoId(nuevo == EstadoProbador.OCUPADO ? datos.usoActivoId() : null);
        probador.setMotivoFueraServicio(nuevo == EstadoProbador.FUERA_DE_SERVICIO ? datos.motivoFueraServicio() : null);
        probador.setFotoEvidencia(nuevo == EstadoProbador.FUERA_DE_SERVICIO ? datos.fotoEvidencia() : null);
        return aRespuesta(probadorRepository.save(probador));
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
        probador.setDesde(reloj.millis());
        return aRespuesta(probadorRepository.save(probador));
    }

    // Solo se elimina un probador que nunca se usó y que no está en uso; debe quedar al menos uno.
    @Transactional
    public void eliminar(String codigo) {
        Probador probador = buscar(codigo);
        if (probadorRepository.count() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sección debe tener al menos un probador");
        }
        if (probador.getEstado() == EstadoProbador.OCUPADO || probador.getEstado() == EstadoProbador.EN_ORDENAMIENTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, codigo + " está en uso: no se puede eliminar ahora");
        }
        if (usoRepository.existsByCodigoProbador(codigo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, codigo + " tiene usos registrados: márquelo fuera de servicio");
        }
        cambioRepository.deleteByCodigoProbador(codigo);
        probadorRepository.delete(probador);
    }

    // --------------------------------- USOS ---------------------------------

    public List<UsoDto> usos() {
        return usoRepository.findAllByOrderByInicioAsc().stream().map(this::aDto).toList();
    }

    // Crea o cierra un uso. El probador debe existir y no se devuelven más prendas de las que entraron.
    public UsoDto guardarUso(String id, UsoDto datos) {
        if (!id.equals(datos.id())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El id de la ruta no coincide con el del cuerpo");
        }
        buscar(datos.codigoProbador());
        if (datos.prendasDevueltas() != null && datos.prendasDevueltas() > datos.prendasIngresadas()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puede devolver más prendas de las que ingresaron");
        }
        if (datos.termino() != null && datos.termino() < datos.inicio()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El término no puede ser anterior al inicio");
        }
        Uso uso = Uso.builder()
                .id(datos.id())
                .codigoProbador(datos.codigoProbador())
                .turnoId(datos.turnoId())
                .inicio(datos.inicio())
                .termino(datos.termino())
                .prendasIngresadas(datos.prendasIngresadas())
                .prendasDevueltas(datos.prendasDevueltas())
                .registroInicio(datos.registroInicio())
                .registroTermino(datos.registroTermino())
                .conAtraso(Boolean.TRUE.equals(datos.conAtraso()))
                .build();
        return aDto(usoRepository.save(uso));
    }

    // ------------------------------- HISTORIAL -------------------------------

    public List<CambioEstadoDto> cambios() {
        return cambioRepository.findAllByOrderByFechaHoraAsc().stream().map(this::aDto).toList();
    }

    // Registra un cambio de estado. Si ya estaba registrado, no lo duplica.
    public CambioEstadoDto registrarCambio(CambioEstadoDto datos) {
        buscar(datos.codigoProbador());
        EstadoProbador nuevo = EstadoProbador.valueOf(datos.nuevo());
        if (!cambioRepository.existsByCodigoProbadorAndFechaHoraAndNuevo(datos.codigoProbador(), datos.fechaHora(), nuevo)) {
            cambioRepository.save(CambioEstado.builder()
                    .codigoProbador(datos.codigoProbador())
                    .anterior(EstadoProbador.valueOf(datos.anterior()))
                    .nuevo(nuevo)
                    .fechaHora(datos.fechaHora())
                    .colaborador(datos.colaborador())
                    .motivo(datos.motivo())
                    .build());
        }
        return datos;
    }

    // ------------------------------------------------------------------------

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
        try {
            return TipoProbador.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo no válido: " + texto);
        }
    }

    private ProbadorResponse aRespuesta(Probador p) {
        return new ProbadorResponse(p.getCodigo(), p.getTipo().name(), p.getEstado().name(), p.getDesde(),
                p.getUsoActivoId(), p.getMotivoFueraServicio(), p.getFotoEvidencia());
    }

    private UsoDto aDto(Uso u) {
        return new UsoDto(u.getId(), u.getCodigoProbador(), u.getTurnoId(), u.getInicio(), u.getTermino(),
                u.getPrendasIngresadas(), u.getPrendasDevueltas(), u.getRegistroInicio(), u.getRegistroTermino(), u.getConAtraso());
    }

    private CambioEstadoDto aDto(CambioEstado c) {
        return new CambioEstadoDto(c.getCodigoProbador(), c.getAnterior().name(), c.getNuevo().name(),
                c.getFechaHora(), c.getColaborador(), c.getMotivo());
    }
}
