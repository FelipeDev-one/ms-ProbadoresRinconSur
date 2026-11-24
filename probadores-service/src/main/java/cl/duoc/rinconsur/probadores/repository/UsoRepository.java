package cl.duoc.rinconsur.probadores.repository;

import cl.duoc.rinconsur.probadores.model.Uso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsoRepository extends JpaRepository<Uso, String> {

    List<Uso> findAllByOrderByInicioAsc();

    boolean existsByCodigoProbador(String codigoProbador);
}
