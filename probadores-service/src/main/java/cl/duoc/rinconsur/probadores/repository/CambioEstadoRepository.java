package cl.duoc.rinconsur.probadores.repository;

import cl.duoc.rinconsur.probadores.model.CambioEstado;
import cl.duoc.rinconsur.probadores.model.EstadoProbador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {

    List<CambioEstado> findAllByOrderByFechaHoraAsc();

    boolean existsByCodigoProbadorAndFechaHoraAndNuevo(String codigoProbador, Long fechaHora, EstadoProbador nuevo);

    void deleteByCodigoProbador(String codigoProbador);
}
