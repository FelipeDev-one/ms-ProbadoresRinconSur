package cl.duoc.rinconsur.usuarios.service;

import cl.duoc.rinconsur.usuarios.dto.ActualizarUsuarioRequest;
import cl.duoc.rinconsur.usuarios.dto.CambiarContrasenaRequest;
import cl.duoc.rinconsur.usuarios.dto.ConfiguracionInicialRequest;
import cl.duoc.rinconsur.usuarios.dto.ContrasenaRequest;
import cl.duoc.rinconsur.usuarios.dto.CrearColaboradorRequest;
import cl.duoc.rinconsur.usuarios.dto.CuentaResponse;
import cl.duoc.rinconsur.usuarios.dto.EstadoInicialResponse;
import cl.duoc.rinconsur.usuarios.dto.LoginRequest;
import cl.duoc.rinconsur.usuarios.dto.RecuperarRequest;
import cl.duoc.rinconsur.usuarios.dto.RegistroClienteRequest;
import cl.duoc.rinconsur.usuarios.dto.UsuarioResponse;
import cl.duoc.rinconsur.usuarios.model.Rol;
import cl.duoc.rinconsur.usuarios.model.TurnoLaboral;
import cl.duoc.rinconsur.usuarios.model.Usuario;
import cl.duoc.rinconsur.usuarios.repository.UsuarioRepository;
import cl.duoc.rinconsur.usuarios.util.CodigoRecuperacion;
import cl.duoc.rinconsur.usuarios.util.RutValidator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

// Reglas de cuentas: mismas que la app (RN-11, RN-13): solo la jefatura crea colaboradores,
// un RUT es cliente o colaborador (no ambos), 5 intentos fallidos bloquean 1 minuto,
// debe quedar al menos una jefatura activa.
@Service
public class UsuarioService {

    public static final int MAX_INTENTOS = 5;
    public static final long MILIS_BLOQUEO = 60_000L;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.reloj = reloj;
    }

    public EstadoInicialResponse estadoInicial() {
        return new EstadoInicialResponse(usuarioRepository.countByRolAndActivoTrue(Rol.JEFATURA) > 0);
    }

    public CuentaResponse configuracionInicial(ConfiguracionInicialRequest datos) {
        if (estadoInicial().configurado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sección ya está configurada");
        }
        Usuario jefa = nuevo(rutValido(datos.rut()), datos.nombre(), Rol.JEFATURA, aTurno(datos.turnoLaboral()));
        return conContrasena(jefa, datos.contrasena());
    }

    public CuentaResponse registrarCliente(RegistroClienteRequest datos) {
        if (!estadoInicial().configurado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La sección aún no está configurada");
        }
        Usuario cliente = nuevo(rutValido(datos.rut()), datos.nombre(), Rol.CLIENTE, null);
        return conContrasena(cliente, datos.contrasena());
    }

    public UsuarioResponse crearColaborador(CrearColaboradorRequest datos) {
        Rol rol = aRol(datos.rol());
        if (!rol.esColaborador()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las cuentas de cliente las crea el propio cliente");
        }
        return aRespuesta(usuarioRepository.save(nuevo(rutValido(datos.rut()), datos.nombre(), rol, aTurno(datos.turnoLaboral()))));
    }

    // rol y buscar son opcionales: filtran por rol y por nombre o RUT.
    public List<UsuarioResponse> listar(String rol, String buscar) {
        List<Usuario> usuarios = (rol == null || rol.isBlank())
                ? usuarioRepository.findAllByOrderByNombreAsc()
                : usuarioRepository.findByRolOrderByNombreAsc(aRol(rol));
        String texto = buscar == null ? "" : buscar.trim().toLowerCase();
        String rutBuscado = texto.replace(".", "").replace("-", "");
        Predicate<Usuario> coincide = u -> texto.isEmpty()
                || u.getNombre().toLowerCase().contains(texto)
                || (!rutBuscado.isEmpty() && u.getRut().toLowerCase().replace("-", "").contains(rutBuscado));
        return usuarios.stream().filter(coincide).map(this::aRespuesta).toList();
    }

    public UsuarioResponse obtener(String rut) {
        return aRespuesta(buscar(rut));
    }

    // Edita los campos que vienen. Un cliente no pasa a colaborador ni al revés.
    public UsuarioResponse actualizar(String rut, ActualizarUsuarioRequest datos) {
        Usuario usuario = buscar(rut);
        if (datos.nombre() != null) usuario.setNombre(limpiar(datos.nombre()));
        if (datos.rol() != null || datos.turnoLaboral() != null) {
            if (usuario.getRol() == Rol.CLIENTE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un cliente no tiene perfil ni turno laboral");
            }
        }
        if (datos.turnoLaboral() != null) usuario.setTurnoLaboral(aTurno(datos.turnoLaboral()));
        boolean dejaDeSerJefaActiva = usuario.getRol() == Rol.JEFATURA && usuario.getActivo()
                && ((datos.rol() != null && aRol(datos.rol()) != Rol.JEFATURA) || Boolean.FALSE.equals(datos.activo()));
        if (dejaDeSerJefaActiva && usuarioRepository.countByRolAndActivoTrue(Rol.JEFATURA) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe quedar al menos una jefatura activa");
        }
        if (datos.rol() != null) usuario.setRol(aRol(datos.rol()));
        if (datos.activo() != null) usuario.setActivo(datos.activo());
        return aRespuesta(usuarioRepository.save(usuario));
    }

    // 401 contraseña incorrecta (con intentos restantes) · 423 bloqueada · 403 inactiva · 409 sin contraseña aún.
    public UsuarioResponse iniciarSesion(LoginRequest datos) {
        Usuario usuario = usuarioRepository.findByRut(RutValidator.normalizar(datos.rut()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "RUT o contraseña incorrectos"));
        if (!usuario.getActivo()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta inactiva: consulte con la jefatura");
        }
        if (usuario.getContrasenaHash() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta aún no tiene contraseña: créela en su primer ingreso");
        }
        verificarConBloqueo(usuario, datos.contrasena(), usuario.getContrasenaHash(), "Contraseña incorrecta");
        return aRespuesta(usuario);
    }

    public CuentaResponse crearContrasena(String rut, ContrasenaRequest datos) {
        Usuario usuario = buscar(rut);
        if (usuario.getContrasenaHash() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya tiene contraseña");
        }
        return conContrasena(usuario, datos.contrasena());
    }

    public CuentaResponse cambiarContrasena(String rut, CambiarContrasenaRequest datos) {
        Usuario usuario = buscar(rut);
        if (usuario.getContrasenaHash() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta aún no tiene contraseña");
        }
        verificarConBloqueo(usuario, datos.actual(), usuario.getContrasenaHash(), "La contraseña actual no es correcta");
        if (datos.actual().equals(datos.nueva())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta de la actual");
        }
        return conContrasena(usuario, datos.nueva());
    }

    // Recupera el acceso con el código de recuperación y entrega un código nuevo.
    public CuentaResponse recuperar(String rut, RecuperarRequest datos) {
        Usuario usuario = buscar(rut);
        if (usuario.getRecuperacionHash() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta no tiene código de recuperación");
        }
        verificarConBloqueo(usuario, CodigoRecuperacion.normalizar(datos.codigo()), usuario.getRecuperacionHash(), "Código incorrecto");
        return conContrasena(usuario, datos.nueva());
    }

    // La jefatura borra la contraseña de un colaborador: la vuelve a crear en su próximo ingreso.
    public void restablecerContrasena(String rut) {
        Usuario usuario = buscar(rut);
        usuario.setContrasenaHash(null);
        usuario.setRecuperacionHash(null);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadaHasta(0L);
        usuarioRepository.save(usuario);
    }

    // ------------------------------------------------------------------------

    private void verificarConBloqueo(Usuario usuario, String texto, String hash, String mensaje) {
        long ahora = reloj.millis();
        if (usuario.getBloqueadaHasta() > ahora) {
            long segundos = (usuario.getBloqueadaHasta() - ahora + 999) / 1000;
            throw new ResponseStatusException(HttpStatus.LOCKED, "Demasiados intentos. Espere " + segundos + " segundos e intente de nuevo.");
        }
        if (passwordEncoder.matches(texto, hash)) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadaHasta(0L);
            usuarioRepository.save(usuario);
            return;
        }
        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos >= MAX_INTENTOS) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadaHasta(ahora + MILIS_BLOQUEO);
            usuarioRepository.save(usuario);
            throw new ResponseStatusException(HttpStatus.LOCKED, "Demasiados intentos. Espere " + MILIS_BLOQUEO / 1000 + " segundos e intente de nuevo.");
        }
        usuario.setIntentosFallidos(intentos);
        usuarioRepository.save(usuario);
        int quedan = MAX_INTENTOS - intentos;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                mensaje + ". Le queda" + (quedan == 1 ? "" : "n") + " " + quedan + " intento" + (quedan == 1 ? "" : "s") + ".");
    }

    private CuentaResponse conContrasena(Usuario usuario, String contrasena) {
        String codigo = CodigoRecuperacion.generar();
        usuario.setContrasenaHash(passwordEncoder.encode(contrasena));
        usuario.setRecuperacionHash(passwordEncoder.encode(CodigoRecuperacion.normalizar(codigo)));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadaHasta(0L);
        return new CuentaResponse(aRespuesta(usuarioRepository.save(usuario)), codigo);
    }

    private Usuario nuevo(String rut, String nombre, Rol rol, TurnoLaboral turno) {
        if (usuarioRepository.existsByRut(rut)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con el RUT " + rut);
        }
        return Usuario.builder()
                .rut(rut)
                .nombre(limpiar(nombre))
                .rol(rol)
                .turnoLaboral(turno)
                .activo(true)
                .creado(LocalDateTime.now(reloj))
                .build();
    }

    private Usuario buscar(String rut) {
        return usuarioRepository.findByRut(RutValidator.normalizar(rut))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una cuenta con el RUT " + rut));
    }

    private String rutValido(String rut) {
        if (!RutValidator.esValido(rut)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RUT inválido: revise el dígito verificador");
        }
        return RutValidator.normalizar(rut);
    }

    private Rol aRol(String texto) {
        try {
            return Rol.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no válido: " + texto);
        }
    }

    private TurnoLaboral aTurno(String texto) {
        try {
            return TurnoLaboral.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turno laboral no válido: " + texto);
        }
    }

    private String limpiar(String nombre) {
        return nombre.trim().replaceAll("\\s+", " ");
    }

    private UsuarioResponse aRespuesta(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getRut(), u.getNombre(), u.getRol().name(),
                u.getTurnoLaboral() == null ? null : u.getTurnoLaboral().name(), u.getActivo(), u.getContrasenaHash() != null);
    }
}
