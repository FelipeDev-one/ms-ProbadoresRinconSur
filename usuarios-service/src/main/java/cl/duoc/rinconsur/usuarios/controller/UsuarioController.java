package cl.duoc.rinconsur.usuarios.controller;

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
import cl.duoc.rinconsur.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Cuentas de clientes y colaboradores")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/estado-inicial")
    @Operation(summary = "¿La sección ya está configurada?", description = "false mientras no exista una jefatura.")
    public EstadoInicialResponse estadoInicial() {
        return usuarioService.estadoInicial();
    }

    @PostMapping("/configuracion-inicial")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea la primera jefatura", description = "Solo funciona mientras no haya una jefatura. Devuelve el código de recuperación.")
    @ApiResponse(responseCode = "201", description = "Jefatura creada")
    @ApiResponse(responseCode = "409", description = "La sección ya está configurada")
    public CuentaResponse configuracionInicial(@Valid @RequestBody ConfiguracionInicialRequest datos) {
        return usuarioService.configuracionInicial(datos);
    }

    @GetMapping
    @Operation(summary = "Lista las cuentas", description = "Filtros opcionales por rol y por nombre o RUT.")
    public List<UsuarioResponse> listar(
            @Parameter(example = "VENDEDOR") @RequestParam(required = false) String rol,
            @Parameter(example = "muñoz") @RequestParam(required = false) String buscar) {
        return usuarioService.listar(rol, buscar);
    }

    @GetMapping("/{rut}")
    @Operation(summary = "Obtiene una cuenta por su RUT")
    @ApiResponse(responseCode = "404", description = "No existe")
    public UsuarioResponse obtener(@Parameter(example = "12984561-9") @PathVariable String rut) {
        return usuarioService.obtener(rut);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una cuenta de cliente", description = "Devuelve el código de recuperación (se muestra una sola vez).")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "400", description = "Datos no válidos o RUT incorrecto")
    @ApiResponse(responseCode = "409", description = "RUT ya registrado o sección sin configurar")
    public CuentaResponse registrarCliente(@Valid @RequestBody RegistroClienteRequest datos) {
        return usuarioService.registrarCliente(datos);
    }

    @PostMapping("/colaboradores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una cuenta de colaborador", description = "Sin contraseña: el colaborador la crea en su primer ingreso.")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "409", description = "RUT ya registrado")
    public UsuarioResponse crearColaborador(@Valid @RequestBody CrearColaboradorRequest datos) {
        return usuarioService.crearColaborador(datos);
    }

    @PutMapping("/{rut}")
    @Operation(summary = "Edita una cuenta", description = "Nombre (todos); perfil, turno y estado (colaboradores).")
    @ApiResponse(responseCode = "400", description = "Datos no válidos")
    @ApiResponse(responseCode = "409", description = "Dejaría sin jefatura activa")
    public UsuarioResponse actualizar(@PathVariable String rut, @Valid @RequestBody ActualizarUsuarioRequest datos) {
        return usuarioService.actualizar(rut, datos);
    }

    @PostMapping("/login")
    @Operation(summary = "Ingreso con RUT y contraseña")
    @ApiResponse(responseCode = "200", description = "Datos de la cuenta")
    @ApiResponse(responseCode = "401", description = "RUT o contraseña incorrectos (indica intentos restantes)")
    @ApiResponse(responseCode = "403", description = "Cuenta inactiva")
    @ApiResponse(responseCode = "409", description = "La cuenta aún no tiene contraseña")
    @ApiResponse(responseCode = "423", description = "Bloqueada 1 minuto por 5 intentos fallidos")
    public UsuarioResponse iniciarSesion(@Valid @RequestBody LoginRequest datos) {
        return usuarioService.iniciarSesion(datos);
    }

    @PostMapping("/{rut}/contrasena")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Primera contraseña de un colaborador", description = "Devuelve el código de recuperación.")
    @ApiResponse(responseCode = "409", description = "La cuenta ya tiene contraseña")
    public CuentaResponse crearContrasena(@PathVariable String rut, @Valid @RequestBody ContrasenaRequest datos) {
        return usuarioService.crearContrasena(rut, datos);
    }

    @PutMapping("/{rut}/contrasena")
    @Operation(summary = "Cambia la contraseña", description = "Pide la actual. Devuelve un código de recuperación nuevo.")
    @ApiResponse(responseCode = "400", description = "La nueva es igual a la actual o no cumple las reglas")
    @ApiResponse(responseCode = "401", description = "La contraseña actual no es correcta")
    public CuentaResponse cambiarContrasena(@PathVariable String rut, @Valid @RequestBody CambiarContrasenaRequest datos) {
        return usuarioService.cambiarContrasena(rut, datos);
    }

    @PostMapping("/{rut}/recuperar")
    @Operation(summary = "Recupera el acceso con el código de recuperación", description = "Devuelve un código nuevo.")
    @ApiResponse(responseCode = "401", description = "Código incorrecto")
    @ApiResponse(responseCode = "423", description = "Bloqueada por intentos fallidos")
    public CuentaResponse recuperar(@PathVariable String rut, @Valid @RequestBody RecuperarRequest datos) {
        return usuarioService.recuperar(rut, datos);
    }

    @DeleteMapping("/{rut}/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Restablece la contraseña (jefatura)", description = "El colaborador la vuelve a crear en su próximo ingreso.")
    public void restablecerContrasena(@PathVariable String rut) {
        usuarioService.restablecerContrasena(rut);
    }
}
