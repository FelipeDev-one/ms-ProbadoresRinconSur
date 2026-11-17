package cl.duoc.rinconsur.probadores.repository;

import cl.duoc.rinconsur.probadores.model.EstadoProbador;
import cl.duoc.rinconsur.probadores.model.Probador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProbadorRepository extends JpaRepository<Probador, Long> {

    List<Probador> findAllByOrderByCodigoAsc();

    List<Probador> findByEstadoOrderByCodigoAsc(EstadoProbador estado);

    Optional<Probador> findByCodigo(String codigo);

    long countByEstado(EstadoProbador estado);
}
