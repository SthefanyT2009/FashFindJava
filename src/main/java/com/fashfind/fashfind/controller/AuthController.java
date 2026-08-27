package com.fashfind.fashfind.controller;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;

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

        if (usuario.getContrasena() == null || usuario.getContrasena().length() < 6) {
            model.addAttribute("error", "La contrasena debe tener al menos 6 caracteres.");
            return "registro";
        }
        if (!usuario.getContrasena().equals(confirmarContrasena)) {
            model.addAttribute("error", "Las contrasenas no coinciden.");
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
}
