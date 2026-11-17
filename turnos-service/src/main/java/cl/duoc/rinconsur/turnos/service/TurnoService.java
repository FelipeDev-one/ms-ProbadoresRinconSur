package cl.duoc.rinconsur.turnos.service;

import cl.duoc.rinconsur.turnos.client.ProbadorClient;
import cl.duoc.rinconsur.turnos.dto.CambiarEstadoRequest;
import cl.duoc.rinconsur.turnos.dto.CrearTurnoRequest;
import cl.duoc.rinconsur.turnos.dto.EsperaResponse;
import cl.duoc.rinconsur.turnos.dto.ResumenProbadoresDto;
import cl.duoc.rinconsur.turnos.dto.TurnoResponse;
import cl.duoc.rinconsur.turnos.model.EstadoTurno;
import cl.duoc.rinconsur.turnos.model.Turno;
import cl.duoc.rinconsur.turnos.repository.TurnoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TurnoService {

    private static final List<EstadoTurno> ABIERTOS = List.of(EstadoTurno.EN_ESPERA, EstadoTurno.LLAMADO);

    private final TurnoRepository turnoRepository;
    private final ProbadorClient probadorClient;
    private final Clock reloj;
    private final int maxPrendas;
    private final int minutosReferencia;

    public TurnoService(
            TurnoRepository turnoRepository,
            ProbadorClient probadorClient,
            Clock reloj,
            @Value("${turnos.max-prendas:6}") int maxPrendas,
            @Value("${turnos.minutos-referencia:8}") int minutosReferencia
    ) {
        this.turnoRepository = turnoRepository;
        this.probadorClient = probadorClient;
        this.reloj = reloj;
        this.maxPrendas = maxPrendas;
        this.minutosReferencia = minutosReferencia;
    }

    public List<TurnoResponse> fila() {
        return turnoRepository.findByFechaAndEstadoInOrderByPreferenteDescNumeroAsc(hoy(), ABIERTOS)
                .stream().map(this::aRespuesta).toList();
    }

    // RN-02: entre 1 y el máximo de prendas. Un cliente no puede tener dos turnos abiertos a la vez.
    public TurnoResponse crear(CrearTurnoRequest datos) {
        if (datos.prendas() == null || datos.prendas() < 1 || datos.prendas() > maxPrendas) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las prendas deben estar entre 1 y " + maxPrendas);
        }
        LocalDate fecha = hoy();
        String rut = (datos.clienteRut() == null || datos.clienteRut().isBlank()) ? null : datos.clienteRut().trim();
        if (rut != null && turnoRepository.existsByFechaAndClienteRutAndEstadoIn(fecha, rut, ABIERTOS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El cliente ya tiene un turno en la fila");
        }
        int numero = turnoRepository.findTopByFechaOrderByNumeroDesc(fecha).map(Turno::getNumero).orElse(0) + 1;
        Turno turno = Turno.builder()
                .numero(numero)
                .fecha(fecha)
                .prendas(datos.prendas())
                .preferente(Boolean.TRUE.equals(datos.preferente()))
                .estado(EstadoTurno.EN_ESPERA)
                .horaIngreso(LocalDateTime.now(reloj))
                .clienteRut(rut)
                .build();
        return aRespuesta(turnoRepository.save(turno));
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
        return new EsperaResponse(
                turno.getCodigo(), turno.getEstado().name(), antes, probadores.libres(),
                estimarMinutos(antes, probadores.libres(), probadores.habilitados()));
    }

    // Llamar, atender o registrar abandono. Un turno cerrado no se reabre (RN-03).
    public TurnoResponse cambiarEstado(Long id, CambiarEstadoRequest datos) {
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
        turno.setEstado(nuevo);
        return aRespuesta(turnoRepository.save(turno));
    }

    // Misma idea que EstimadorEspera de la app: si hay probador libre y nadie antes, no hay espera;
    // si no, cada "ronda" de probadores habilitados demora el tiempo de referencia.
    public int estimarMinutos(int personasAntes, long libres, long habilitados) {
        if (libres > 0 && personasAntes == 0) return 0;
        long probadores = Math.max(habilitados, 1);
        long rondas = (personasAntes + probadores) / probadores;
        return (int) (rondas * minutosReferencia);
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private TurnoResponse aRespuesta(Turno t) {
        return new TurnoResponse(t.getId(), t.getCodigo(), t.getNumero(), t.getPrendas(), t.getPreferente(),
                t.getEstado().name(), t.getHoraIngreso(), t.getClienteRut());
    }
}
