package cl.duoc.rinconsur.turnos.service;

import cl.duoc.rinconsur.turnos.client.ProbadorClient;
import cl.duoc.rinconsur.turnos.client.UsuarioClient;
import cl.duoc.rinconsur.turnos.dto.CambiarEstadoRequest;
import cl.duoc.rinconsur.turnos.dto.CrearTurnoRequest;
import cl.duoc.rinconsur.turnos.dto.EsperaResponse;
import cl.duoc.rinconsur.turnos.dto.GuardarTurnoRequest;
import cl.duoc.rinconsur.turnos.dto.ParametrosDto;
import cl.duoc.rinconsur.turnos.dto.ResumenProbadoresDto;
import cl.duoc.rinconsur.turnos.dto.TurnoResponse;
import cl.duoc.rinconsur.turnos.model.EstadoTurno;
import cl.duoc.rinconsur.turnos.model.MotivoAbandono;
import cl.duoc.rinconsur.turnos.model.Parametros;
import cl.duoc.rinconsur.turnos.model.Turno;
import cl.duoc.rinconsur.turnos.repository.ParametrosRepository;
import cl.duoc.rinconsur.turnos.repository.TurnoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TurnoService {

    private static final List<EstadoTurno> ABIERTOS = List.of(EstadoTurno.EN_ESPERA, EstadoTurno.LLAMADO);

    private final TurnoRepository turnoRepository;
    private final ParametrosRepository parametrosRepository;
    private final ProbadorClient probadorClient;
    private final UsuarioClient usuarioClient;
    private final Clock reloj;

    public TurnoService(TurnoRepository turnoRepository, ParametrosRepository parametrosRepository,
                        ProbadorClient probadorClient, UsuarioClient usuarioClient, Clock reloj) {
        this.turnoRepository = turnoRepository;
        this.parametrosRepository = parametrosRepository;
        this.probadorClient = probadorClient;
        this.usuarioClient = usuarioClient;
        this.reloj = reloj;
    }

    // ------------------------------- CONSULTAS -------------------------------

    public List<TurnoResponse> fila() {
        return turnoRepository.findByFechaAndEstadoInOrderByPreferenteDescNumeroAsc(hoy(), ABIERTOS)
                .stream().map(this::aRespuesta).toList();
    }

    // Todos los turnos (o los de una fecha): la app los descarga al sincronizar.
    public List<TurnoResponse> listar(LocalDate fecha) {
        List<Turno> turnos = fecha == null
                ? turnoRepository.findAllByOrderByFechaAscNumeroAsc()
                : turnoRepository.findByFechaOrderByNumeroAsc(fecha);
        return turnos.stream().map(this::aRespuesta).toList();
    }

    // El turno abierto de un cliente hoy (lo usa la app para mostrar "Su turno").
    public TurnoResponse turnoDeCliente(String rut) {
        return turnoRepository.findFirstByFechaAndClienteRutAndEstadoIn(hoy(), rut.trim().toUpperCase(), ABIERTOS)
                .map(this::aRespuesta)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El cliente no tiene un turno en la fila"));
    }

    // Consulta del cliente: cuántas personas tiene antes y cuánto falta (pide los probadores a probadores-service).
    public EsperaResponse espera(int numero) {
        Turno turno = turnoRepository.findByFechaAndNumero(hoy(), numero)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el turno " + numero + " hoy"));
        if (!turno.getEstado().abierto()) {
            return new EsperaResponse(turno.getCodigo(), turno.getEstado().name(), 0, 0, null);
        }
        List<Turno> fila = turnoRepository.findByFechaAndEstadoInOrderByPreferenteDescNumeroAsc(hoy(), ABIERTOS);
        int antes = 0;
        for (Turno t : fila) {
            if (t.getId().equals(turno.getId())) break;
            antes++;
        }
        ResumenProbadoresDto probadores = probadorClient.resumen();
        return new EsperaResponse(turno.getCodigo(), turno.getEstado().name(), antes, probadores.libres(),
                estimarMinutos(antes, probadores.libres(), probadores.habilitados()));
    }

    // -------------------------------- ESCRITURA --------------------------------

    // RN-02: entre 1 y el máximo de prendas. Si viene RUT, debe ser un cliente activo
    // (se consulta a usuarios-service) y no puede tener otro turno abierto.
    public TurnoResponse crear(CrearTurnoRequest datos) {
        validarPrendas(datos.prendas());
        LocalDate fecha = hoy();
        String rut = normalizar(datos.clienteRut());
        if (rut != null) {
            validarCliente(rut, fecha, null);
        }
        int numero = turnoRepository.findTopByFechaOrderByNumeroDesc(fecha).map(Turno::getNumero).orElse(0) + 1;
        Turno turno = Turno.builder()
                .id(UUID.randomUUID().toString())
                .numero(numero)
                .fecha(fecha)
                .horaIngreso(reloj.millis())
                .preferente(Boolean.TRUE.equals(datos.preferente()))
                .prendas(datos.prendas())
                .estado(EstadoTurno.EN_ESPERA)
                .registradoPor(rut == null ? normalizar(datos.registradoPor()) : null)
                .clienteRut(rut)
                .build();
        return aRespuesta(turnoRepository.save(turno));
    }

    // Crea o actualiza un turno completo (sincronización desde la app).
    // Se validan las mismas reglas: prendas, número único por día, cliente activo y RN-03.
    public TurnoResponse guardar(String id, GuardarTurnoRequest datos) {
        validarPrendas(datos.prendas());
        EstadoTurno nuevo = EstadoTurno.valueOf(datos.estado());
        Turno existente = turnoRepository.findById(id).orElse(null);
        turnoRepository.findByFechaAndNumero(datos.fecha(), datos.numero())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya existe el turno " + otro.getCodigo() + " del " + datos.fecha());
                });
        String rut = normalizar(datos.clienteRut());
        if (existente == null) {
            if (rut != null && nuevo.abierto()) {
                validarCliente(rut, datos.fecha(), id);
            }
            existente = Turno.builder().id(id).build();
        } else if (!existente.getEstado().abierto() && existente.getEstado() != nuevo) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, existente.getCodigo() + " ya está cerrado");
        }
        existente.setNumero(datos.numero());
        existente.setFecha(datos.fecha());
        existente.setHoraIngreso(datos.horaIngreso());
        existente.setPreferente(Boolean.TRUE.equals(datos.preferente()));
        existente.setPrendas(datos.prendas());
        existente.setEstado(nuevo);
        existente.setHoraLlamado(datos.horaLlamado());
        existente.setHoraCierre(datos.horaCierre());
        existente.setMotivoAbandono(datos.motivoAbandono() == null ? null : MotivoAbandono.valueOf(datos.motivoAbandono()));
        existente.setRegistradoPor(normalizar(datos.registradoPor()));
        existente.setClienteRut(rut);
        return aRespuesta(turnoRepository.save(existente));
    }

    // Llamar, atender, abandono o no se presentó. Un turno cerrado no se reabre (RN-03).
    public TurnoResponse cambiarEstado(String id, CambiarEstadoRequest datos) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el turno"));
        EstadoTurno nuevo;
        try {
            nuevo = EstadoTurno.valueOf(datos.estado().trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido: " + datos.estado());
        }
        if (!turno.getEstado().abierto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, turno.getCodigo() + " ya está cerrado");
        }
        if (nuevo == EstadoTurno.EN_ESPERA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un turno no vuelve a quedar en espera");
        }
        long ahora = reloj.millis();
        turno.setEstado(nuevo);
        if (nuevo == EstadoTurno.LLAMADO) {
            turno.setHoraLlamado(ahora);
        } else {
            turno.setHoraCierre(ahora);
        }
        if (nuevo == EstadoTurno.ABANDONO) {
            turno.setMotivoAbandono(datos.motivo() == null || datos.motivo().isBlank()
                    ? MotivoAbandono.OTRO : MotivoAbandono.valueOf(datos.motivo().trim().toUpperCase()));
        } else if (nuevo == EstadoTurno.NO_SE_PRESENTO) {
            turno.setMotivoAbandono(MotivoAbandono.NO_INFORMADO);
        }
        return aRespuesta(turnoRepository.save(turno));
    }

    // ------------------------------- PARÁMETROS -------------------------------

    public ParametrosDto parametros() {
        return aDto(parametrosActuales());
    }

    public ParametrosDto guardarParametros(ParametrosDto datos) {
        if (datos.horaCierre() <= datos.horaApertura()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora de cierre debe ser posterior a la de apertura");
        }
        if (datos.umbralAyuda() <= datos.umbralProlongado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El umbral de ayuda debe ser mayor que el de uso prolongado");
        }
        Parametros p = new Parametros(Parametros.ID_UNICO, datos.maxPrendas(), datos.tiempoReferenciaUso(),
                datos.minutosOrdenamiento(), datos.umbralProlongado(), datos.umbralAyuda(), datos.minutosPresentarse(),
                datos.ticketPromedio(), datos.horaApertura(), datos.horaCierre(), datos.minutosCierreFila());
        return aDto(parametrosRepository.save(p));
    }

    // Misma idea que EstimadorEspera de la app: si hay probador libre y nadie antes, no hay espera;
    // si no, cada "ronda" de probadores habilitados demora el tiempo de referencia.
    public int estimarMinutos(int personasAntes, long libres, long habilitados) {
        if (libres > 0 && personasAntes == 0) return 0;
        long probadores = Math.max(habilitados, 1);
        long rondas = (personasAntes + probadores) / probadores;
        return (int) (rondas * parametrosActuales().getTiempoReferenciaUso());
    }

    // --------------------------------------------------------------------------

    private Parametros parametrosActuales() {
        return parametrosRepository.findById(Parametros.ID_UNICO).orElseGet(Parametros::porDefecto);
    }

    private void validarPrendas(Integer prendas) {
        int max = parametrosActuales().getMaxPrendas();
        if (prendas == null || prendas < 1 || prendas > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las prendas deben estar entre 1 y " + max);
        }
    }

    private void validarCliente(String rut, LocalDate fecha, String idPropio) {
        if (!usuarioClient.esClienteActivo(rut)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay una cuenta de cliente activa con el RUT " + rut);
        }
        boolean otroAbierto = turnoRepository.findFirstByFechaAndClienteRutAndEstadoIn(fecha, rut, ABIERTOS)
                .filter(t -> !Objects.equals(t.getId(), idPropio))
                .isPresent();
        if (otroAbierto) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El cliente ya tiene un turno en la fila");
        }
    }

    private static String normalizar(String rut) {
        return (rut == null || rut.isBlank()) ? null : rut.trim().toUpperCase();
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private TurnoResponse aRespuesta(Turno t) {
        return new TurnoResponse(t.getId(), t.getCodigo(), t.getNumero(), t.getFecha(), t.getHoraIngreso(),
                t.getPreferente(), t.getPrendas(), t.getEstado().name(), t.getHoraLlamado(), t.getHoraCierre(),
                t.getMotivoAbandono() == null ? null : t.getMotivoAbandono().name(), t.getRegistradoPor(), t.getClienteRut());
    }

    private ParametrosDto aDto(Parametros p) {
        return new ParametrosDto(p.getMaxPrendas(), p.getTiempoReferenciaUso(), p.getMinutosOrdenamiento(),
                p.getUmbralProlongado(), p.getUmbralAyuda(), p.getMinutosPresentarse(), p.getTicketPromedio(),
                p.getHoraApertura(), p.getHoraCierre(), p.getMinutosCierreFila());
    }
}
