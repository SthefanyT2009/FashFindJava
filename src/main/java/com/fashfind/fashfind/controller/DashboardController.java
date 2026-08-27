package com.fashfind.fashfind.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fashfind.fashfind.entity.Usuario;
import com.fashfind.fashfind.repository.UsuarioRepository;

@Controller
public class DashboardController {

    private final UsuarioRepository usuarioRepository;

    public DashboardController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/inicio";
    }

    @GetMapping("/inicio")
    public String inicio(Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        return "inicio";
    }

    @GetMapping("/cliente-dashboard")
    public String clienteDashboard(Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        return "cliente-dashboard";
    }

    @GetMapping("/vendedor-dashboard")
    public String vendedorDashboard(Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        return "vendedor-dashboard";
    }

    @GetMapping("/domiciliario-dashboard")
    public String domiciliarioDashboard(Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        return "domiciliario-dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        return "admin-dashboard";
    }

    private void agregarUsuarioAlModelo(Model model, Authentication authentication) {
        if (authentication == null) {
            return;
        }
        Usuario usuario = usuarioRepository.findByNombreUsuario(authentication.getName()).orElse(null);
        model.addAttribute("usuarioActual", usuario);
    }
}