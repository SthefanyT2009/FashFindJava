package com.fashfind.fashfind.controller;

import java.beans.PropertyEditorSupport;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fashfind.fashfind.entity.Cargo;
import com.fashfind.fashfind.entity.Genero;
import com.fashfind.fashfind.entity.Usuario;
import com.fashfind.fashfind.repository.UsuarioRepository;
import com.fashfind.fashfind.service.UsuarioService;

/**
 * Gestion de Usuarios del administrador: alta, edicion, baja logica y
 * reactivacion de cuentas de cualquier cargo (Administrador, Vendedor,
 * Domiciliario, Cliente). Protegido en SecurityConfig con
 * hasRole("ADMINISTRADOR").
 */
@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    /** Mismo tratamiento que AuthController para el select de genero opcional. */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Genero.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue((text == null || text.isBlank()) ? null : Genero.valueOf(text));
            }
        });
        // El select de cargo incluye una opcion vacia ("Selecciona un cargo") solo
        // para forzar al usuario a elegir uno; si de todos modos llega vacio (por
        // ejemplo con JS deshabilitado), lo tratamos como null y lo reporta la
        // validacion del servicio con un mensaje claro, en vez de romper el binding.
        binder.registerCustomEditor(Cargo.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue((text == null || text.isBlank()) ? null : Cargo.valueOf(text));
            }
        });
    }

    @GetMapping("/usuarios")
    public String listar(@RequestParam(required = false) String error, @RequestParam(required = false) String exito,
                          Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        model.addAttribute("usuarios", usuarioService.listarUsuarios());
        if (error != null) {
            model.addAttribute("error", error);
        }
        if (exito != null) {
            model.addAttribute("exito", exito);
        }
        return "usuarios/usuarios";
    }

    @GetMapping("/usuarios/nuevo")
    public String formNuevo(@RequestParam(required = false) String error, Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("esNuevo", true);
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "usuarios/usuario-form";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String formEditar(@PathVariable Integer id, @RequestParam(required = false) String error,
                              Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        model.addAttribute("usuario", usuarioService.obtenerPorId(id));
        model.addAttribute("esNuevo", false);
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "usuarios/usuario-form";
    }

    @PostMapping("/usuarios")
    public String crear(@ModelAttribute Usuario usuario,
                         @RequestParam("confirmarContrasena") String confirmarContrasena) {
        try {
            usuarioService.crearUsuario(usuario, confirmarContrasena);
            return "redirect:/usuarios?exito=" + encodar("Usuario creado con exito.");
        } catch (RuntimeException e) {
            return "redirect:/usuarios/nuevo?error=" + encodar(e.getMessage());
        }
    }

    @PostMapping("/usuarios/actualizar/{id}")
    public String actualizar(@PathVariable Integer id,
                              @ModelAttribute Usuario usuario,
                              @RequestParam(value = "nuevaContrasena", required = false) String nuevaContrasena,
                              @RequestParam(value = "confirmarContrasena", required = false) String confirmarContrasena,
                              Authentication authentication) {
        try {
            Usuario usuarioAutenticado = usuarioActual(authentication);
            usuarioService.actualizarUsuario(id, usuario, nuevaContrasena, confirmarContrasena, usuarioAutenticado);
            return "redirect:/usuarios?exito=" + encodar("Usuario actualizado con exito.");
        } catch (RuntimeException e) {
            return "redirect:/usuarios/editar/" + id + "?error=" + encodar(e.getMessage());
        }
    }

    @PostMapping("/usuarios/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, Authentication authentication) {
        try {
            usuarioService.eliminarUsuario(id, usuarioActual(authentication));
        } catch (RuntimeException e) {
            return "redirect:/usuarios?error=" + encodar(e.getMessage());
        }
        return "redirect:/usuarios?exito=" + encodar("Usuario desactivado con exito.");
    }

    @PostMapping("/usuarios/reactivar/{id}")
    public String reactivar(@PathVariable Integer id) {
        try {
            usuarioService.reactivarUsuario(id);
        } catch (RuntimeException e) {
            return "redirect:/usuarios?error=" + encodar(e.getMessage());
        }
        return "redirect:/usuarios?exito=" + encodar("Usuario reactivado con exito.");
    }

    private Usuario usuarioActual(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return usuarioRepository.findByNombreUsuario(authentication.getName()).orElse(null);
    }

    private void agregarUsuarioAlModelo(Model model, Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);
        if (usuario != null) {
            model.addAttribute("usuarioActual", usuario);
        }
    }

    private String encodar(String mensaje) {
        if (mensaje == null) {
            mensaje = "Ocurrio un error inesperado.";
        }
        return java.net.URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }
}