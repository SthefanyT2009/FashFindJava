package com.fashfind.fashfind.controller;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fashfind.fashfind.entity.Cargo;
import com.fashfind.fashfind.entity.Genero;
import com.fashfind.fashfind.entity.Usuario;
import com.fashfind.fashfind.repository.UsuarioRepository;

/**
 * Controlador de autenticacion. El login en si lo maneja Spring Security
 * (formLogin apuntando a /login), esta clase solo sirve las vistas y el
 * registro de nuevos clientes.
 *
 * El auto-registro solo crea usuarios con cargo "Cliente": los roles
 * Administrador, Vendedor y Domiciliario los asigna un administrador desde
 * la gestion de usuarios, no se auto-asignan desde el formulario publico.
 */
@Controller
public class AuthController {

    // Misma regla que UsuarioService: minimo 6 caracteres, sin espacios, con
    // al menos una minuscula, una mayuscula, un numero y un simbolo.
    private static final Pattern PATRON_CONTRASENA = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{6,}$");
    private static final String MENSAJE_CONTRASENA =
            "La contrasena debe tener minimo 6 caracteres, sin espacios, e incluir al menos "
                    + "una mayuscula, una minuscula, un numero y un simbolo.";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * El select de genero incluye una opcion vacia ("Prefiero no decirlo").
     * Sin este editor, Spring lanza una excepcion al intentar convertir un
     * String vacio al enum Genero; aqui lo tratamos explicitamente como null.
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Genero.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue((text == null || text.isBlank()) ? null : Genero.valueOf(text));
            }
        });
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute("usuario") Usuario usuario,
                                    @RequestParam("confirmarContrasena") String confirmarContrasena,
                                    Model model) {

        if (usuario.getContrasena() == null || !PATRON_CONTRASENA.matcher(usuario.getContrasena()).matches()) {
            model.addAttribute("error", MENSAJE_CONTRASENA);
            return "registro";
        }
        if (!usuario.getContrasena().equals(confirmarContrasena)) {
            model.addAttribute("error", "Las contrasenas no coinciden.");
            return "registro";
        }
        if (usuario.getFechaNacimiento() == null || usuario.getFechaNacimiento().isAfter(LocalDate.now())) {
            model.addAttribute("error", "La fecha de nacimiento no puede ser futura.");
            return "registro";
        }
        int edad = Period.between(usuario.getFechaNacimiento(), LocalDate.now()).getYears();
        if (edad < 12 || edad > 100) {
            model.addAttribute("error", "Debes tener entre 12 y 100 anos.");
            return "registro";
        }
        if (usuario.getGenero() == null) {
            model.addAttribute("error", "Selecciona un genero: Femenino o Masculino.");
            return "registro";
        }
        if (usuarioRepository.existsByNombreUsuario(usuario.getNombreUsuario())) {
            model.addAttribute("error", "Ese nombre de usuario ya esta en uso.");
            return "registro";
        }
        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) {
            model.addAttribute("error", "Ya existe una cuenta registrada con ese correo.");
            return "registro";
        }
        if (usuario.getCc() != null && usuarioRepository.existsByCc(usuario.getCc())) {
            model.addAttribute("error", "Ya existe una cuenta registrada con esa cedula.");
            return "registro";
        }

        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        usuario.setCargo(Cargo.Cliente);
        usuario.setEstado("Activo");
        usuario.setFechaRegistro(LocalDate.now());

        usuarioRepository.save(usuario);

        return "redirect:/login?registrado";
    }

    /**
     * Endpoint publico (sin autenticacion) que usan tanto registro.html
     * como usuario-form.html para avisar en vivo, mientras se escribe, si
     * el nombre de usuario ya esta en uso. "id" se envia solo desde el
     * formulario de edicion de un administrador, para excluir el propio
     * registro de la comprobacion.
     */
    @GetMapping("/api/usuarios/existe-usuario")
    @ResponseBody
    public Map<String, Boolean> existeNombreUsuario(@RequestParam String nombreUsuario,
                                                      @RequestParam(required = false) Integer id) {
        String valor = nombreUsuario == null ? "" : nombreUsuario.trim();
        boolean existe = !valor.isEmpty() && (id == null
                ? usuarioRepository.existsByNombreUsuario(valor)
                : usuarioRepository.existsByNombreUsuarioAndIdUsuarioNot(valor, id));
        return Map.of("existe", existe);
    }
}