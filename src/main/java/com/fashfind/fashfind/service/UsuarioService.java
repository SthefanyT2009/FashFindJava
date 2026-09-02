package com.fashfind.fashfind.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fashfind.fashfind.entity.Cargo;
import com.fashfind.fashfind.entity.Usuario;
import com.fashfind.fashfind.repository.UsuarioRepository;

/**
 * Gestiona el ciclo de vida de las cuentas de Usuario desde la Gestion de
 * Usuarios del administrador (a diferencia de AuthController, que solo
 * cubre el auto-registro publico de Clientes, aqui un administrador puede
 * crear o editar cuentas con cualquier cargo).
 *
 * Todas las reglas de validacion espejan las que ya se usan en el registro
 * publico (registro.html / AuthController), mas las validaciones de
 * unicidad al editar (que excluyen el propio registro) y un par de
 * resguardos para que un administrador no se bloquee a si mismo.
 */
@Service
public class UsuarioService {

    private static final Pattern PATRON_USUARIO = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final Pattern PATRON_CEDULA = Pattern.compile("^[0-9]{6,10}$");
    private static final Pattern PATRON_NOMBRE = Pattern.compile("^[A-Za-zÁÉÍÓÚÑÜáéíóúñü\\s]{2,50}$");
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^[0-9]{7,10}$");
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    // Minimo 6 caracteres, sin espacios, con al menos una minuscula, una
    // mayuscula, un numero y un simbolo.
    private static final Pattern PATRON_CONTRASENA = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{6,}$");
    private static final String MENSAJE_CONTRASENA =
            "La contrasena debe tener minimo 6 caracteres, sin espacios, e incluir al menos "
                    + "una mayuscula, una minuscula, un numero y un simbolo.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll(Sort.by(Sort.Direction.ASC, "nombres"));
    }

    public Usuario obtenerPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
    }

    /**
     * Crea una cuenta nueva con cualquier cargo. La contrasena es obligatoria
     * y se cifra antes de guardar.
     */
    @Transactional
    public Usuario crearUsuario(Usuario datos, String confirmarContrasena) {
        validarCamposComunes(datos, null);

        if (datos.getContrasena() == null || !PATRON_CONTRASENA.matcher(datos.getContrasena()).matches()) {
            throw new IllegalArgumentException(MENSAJE_CONTRASENA);
        }
        if (!datos.getContrasena().equals(confirmarContrasena)) {
            throw new IllegalArgumentException("Las contrasenas no coinciden.");
        }

        Usuario usuario = new Usuario();
        copiarCamposComunes(datos, usuario, true);
        usuario.setContrasena(passwordEncoder.encode(datos.getContrasena()));
        usuario.setEstado("Activo");
        usuario.setFechaRegistro(LocalDate.now());

        return usuarioRepository.save(usuario);
    }

    /**
     * Actualiza una cuenta existente. La contrasena es opcional: si se deja
     * en blanco, se conserva la actual; si se informa, debe cumplir las
     * mismas reglas que al crear.
     *
     * usuarioAutenticado es la cuenta con la que esta trabajando el
     * administrador que hace el cambio; se usa para evitar que se quite a
     * si mismo el rol de Administrador o se desactive por error.
     */
    @Transactional
    public Usuario actualizarUsuario(Integer id, Usuario datos, String nuevaContrasena,
                                      String confirmarContrasena, Usuario usuarioAutenticado) {
        Usuario usuario = obtenerPorId(id);
        validarCamposComunes(datos, id);

        boolean esElMismo = usuarioAutenticado != null && usuarioAutenticado.getIdUsuario().equals(id);
        if (esElMismo && datos.getCargo() != Cargo.Administrador) {
            throw new IllegalArgumentException("No puedes quitarte a ti mismo el cargo de Administrador.");
        }

        if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
            if (!PATRON_CONTRASENA.matcher(nuevaContrasena).matches()) {
                throw new IllegalArgumentException(MENSAJE_CONTRASENA);
            }
            if (!nuevaContrasena.equals(confirmarContrasena)) {
                throw new IllegalArgumentException("Las contrasenas no coinciden.");
            }
            usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        }

        copiarCamposComunes(datos, usuario, false);
        return usuarioRepository.save(usuario);
    }

    /**
     * Baja logica: pone la cuenta en "Inactivo". No permite que un
     * administrador se desactive a si mismo (se quedaria sin acceso a esta
     * misma pantalla para revertirlo).
     */
    @Transactional
    public void eliminarUsuario(Integer id, Usuario usuarioAutenticado) {
        if (usuarioAutenticado != null && usuarioAutenticado.getIdUsuario().equals(id)) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
        }
        Usuario usuario = obtenerPorId(id);
        usuario.setEstado("Inactivo");
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void reactivarUsuario(Integer id) {
        Usuario usuario = obtenerPorId(id);
        usuario.setEstado("Activo");
        usuarioRepository.save(usuario);
    }

    // =========================================
    // Validaciones
    // =========================================

    private void validarCamposComunes(Usuario datos, Integer idAEditar) {
        boolean esNuevo = idAEditar == null;

        if (datos.getNombreUsuario() == null || !PATRON_USUARIO.matcher(datos.getNombreUsuario().trim()).matches()) {
            throw new IllegalArgumentException(
                    "El nombre de usuario debe tener entre 3 y 20 caracteres (letras, numeros y guion bajo).");
        }

        // Nombres, apellidos y cedula solo se validan (y se pueden fijar) al
        // crear la cuenta. Al editar, estos tres campos son de solo lectura:
        // no llegan a cambiar, asi que no tiene sentido revalidarlos ni
        // volver a comprobar duplicados de cedula (ver copiarCamposComunes).
        if (esNuevo) {
            if (datos.getCc() == null || !PATRON_CEDULA.matcher(String.valueOf(datos.getCc())).matches()) {
                throw new IllegalArgumentException("La cedula debe tener entre 6 y 10 digitos.");
            }
            if (datos.getNombres() == null || !PATRON_NOMBRE.matcher(datos.getNombres().trim()).matches()) {
                throw new IllegalArgumentException("El nombre debe tener entre 2 y 50 caracteres, solo letras y espacios.");
            }
            if (datos.getApellidos() == null || !PATRON_NOMBRE.matcher(datos.getApellidos().trim()).matches()) {
                throw new IllegalArgumentException("Los apellidos deben tener entre 2 y 50 caracteres, solo letras y espacios.");
            }
        }
        if (datos.getCorreo() == null || !PATRON_CORREO.matcher(datos.getCorreo().trim()).matches()
                || datos.getCorreo().trim().length() > 100) {
            throw new IllegalArgumentException("Ingresa un correo valido, ej: nombre@correo.com");
        }
        if (datos.getTelefono() == null || !PATRON_TELEFONO.matcher(String.valueOf(datos.getTelefono())).matches()) {
            throw new IllegalArgumentException("El telefono debe tener entre 7 y 10 digitos.");
        }
        if (datos.getDireccion() == null || datos.getDireccion().trim().length() < 5
                || datos.getDireccion().trim().length() > 100) {
            throw new IllegalArgumentException("La direccion debe tener entre 5 y 100 caracteres.");
        }
        if (datos.getFechaNacimiento() == null || datos.getFechaNacimiento().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura.");
        }
        int edad = Period.between(datos.getFechaNacimiento(), LocalDate.now()).getYears();
        if (edad < 12 || edad > 100) {
            throw new IllegalArgumentException("La edad debe estar entre 12 y 100 anos.");
        }
        if (datos.getGenero() == null) {
            throw new IllegalArgumentException("Selecciona un genero: Femenino o Masculino.");
        }
        if (datos.getCargo() == null) {
            throw new IllegalArgumentException("Debes seleccionar un cargo.");
        }

        boolean usuarioDuplicado = esNuevo
                ? usuarioRepository.existsByNombreUsuario(datos.getNombreUsuario().trim())
                : usuarioRepository.existsByNombreUsuarioAndIdUsuarioNot(datos.getNombreUsuario().trim(), idAEditar);
        if (usuarioDuplicado) {
            throw new IllegalArgumentException("Ese nombre de usuario ya esta en uso.");
        }

        boolean correoDuplicado = esNuevo
                ? usuarioRepository.existsByCorreo(datos.getCorreo().trim())
                : usuarioRepository.existsByCorreoAndIdUsuarioNot(datos.getCorreo().trim(), idAEditar);
        if (correoDuplicado) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con ese correo.");
        }

        // La cedula solo se comprueba al crear: al editar el campo no
        // cambia, asi que no puede chocar consigo mismo.
        if (esNuevo && usuarioRepository.existsByCc(datos.getCc())) {
            throw new IllegalArgumentException("Ya existe una cuenta registrada con esa cedula.");
        }
    }

    /**
     * Copia los campos comunes desde el formulario hacia la entidad.
     * Al editar (esNuevo = false) se ignoran nombres, apellidos y cedula
     * aunque lleguen en el formulario: esos tres campos son de solo lectura
     * una vez creada la cuenta, y el valor que ya tiene "destino" (cargado
     * desde la base de datos) se conserva tal cual.
     */
    private void copiarCamposComunes(Usuario datos, Usuario destino, boolean esNuevo) {
        destino.setNombreUsuario(datos.getNombreUsuario().trim());
        if (esNuevo) {
            destino.setCc(datos.getCc());
            destino.setNombres(datos.getNombres().trim());
            destino.setApellidos(datos.getApellidos().trim());
        }
        destino.setGenero(datos.getGenero());
        destino.setFechaNacimiento(datos.getFechaNacimiento());
        destino.setCorreo(datos.getCorreo().trim());
        destino.setTelefono(datos.getTelefono());
        destino.setDireccion(datos.getDireccion().trim());
        destino.setCargo(datos.getCargo());
    }
}