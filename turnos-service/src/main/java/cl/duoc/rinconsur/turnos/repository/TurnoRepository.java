package cl.duoc.rinconsur.turnos.repository;

import cl.duoc.rinconsur.turnos.model.EstadoTurno;
import cl.duoc.rinconsur.turnos.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TurnoRepository extends JpaRepository<Turno, String> {

    // Fila del día: preferentes primero y después por número.
    List<Turno> findByFechaAndEstadoInOrderByPreferenteDescNumeroAsc(LocalDate fecha, Collection<EstadoTurno> estados);

    List<Turno> findByFechaOrderByNumeroAsc(LocalDate fecha);

    List<Turno> findAllByOrderByFechaAscNumeroAsc();

    Optional<Turno> findTopByFechaOrderByNumeroDesc(LocalDate fecha);

    Optional<Turno> findByFechaAndNumero(LocalDate fecha, Integer numero);

    boolean existsByFechaAndClienteRutAndEstadoIn(LocalDate fecha, String clienteRut, Collection<EstadoTurno> estados);

    Optional<Turno> findFirstByFechaAndClienteRutAndEstadoIn(LocalDate fecha, String clienteRut, Collection<EstadoTurno> estados);
}
