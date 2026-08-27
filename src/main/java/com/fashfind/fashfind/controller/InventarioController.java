package com.fashfind.fashfind.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fashfind.fashfind.entity.Usuario;
import com.fashfind.fashfind.repository.UsuarioRepository;
import com.fashfind.fashfind.service.InventarioService;

@Controller
public class InventarioController {

    private final InventarioService inventarioService;
    private final UsuarioRepository usuarioRepository;

    public InventarioController(InventarioService inventarioService, UsuarioRepository usuarioRepository) {
        this.inventarioService = inventarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/inventario")
    public String listar(Model model, Authentication authentication) {
        if (authentication != null) {
            Usuario usuario = usuarioRepository.findByNombreUsuario(authentication.getName()).orElse(null);
            model.addAttribute("usuarioActual", usuario);
        }
        model.addAttribute("inventarios", inventarioService.listarInventario());
        return "inventario";
    }

    @PostMapping("/inventario/actualizar/{id}")
    public String actualizar(@PathVariable Integer id,
                              @RequestParam Integer stockDisponible,
                              @RequestParam Integer stockMinimo) {
        inventarioService.actualizarStock(id, stockDisponible, stockMinimo);
        return "redirect:/inventario";
    }
}