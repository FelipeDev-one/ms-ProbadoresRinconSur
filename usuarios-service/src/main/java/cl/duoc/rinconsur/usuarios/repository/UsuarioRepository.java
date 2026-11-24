package cl.duoc.rinconsur.usuarios.repository;

import cl.duoc.rinconsur.usuarios.model.Rol;
import cl.duoc.rinconsur.usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByRut(String rut);

    boolean existsByRut(String rut);

    List<Usuario> findAllByOrderByNombreAsc();

    List<Usuario> findByRolOrderByNombreAsc(Rol rol);

    long countByRolAndActivoTrue(Rol rol);
}
