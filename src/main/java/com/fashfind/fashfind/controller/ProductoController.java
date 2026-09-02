package com.fashfind.fashfind.controller;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fashfind.fashfind.entity.Producto;
import com.fashfind.fashfind.entity.Usuario;
import com.fashfind.fashfind.repository.UsuarioRepository;
import com.fashfind.fashfind.service.ProductoService;

@Controller
public class ProductoController {

    private final ProductoService productoService;
    private final UsuarioRepository usuarioRepository;

    public ProductoController(ProductoService productoService, UsuarioRepository usuarioRepository) {
        this.productoService = productoService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/productos")
    public String listar(@RequestParam(required = false) String exito, Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        model.addAttribute("productos", productoService.listarProductos());
        if (exito != null) {
            model.addAttribute("exito", exito);
        }
<<<<<<< HEAD
        return "productos/productos";
=======
        return "productos";
>>>>>>> 4903f414195c0ab27cab724f1199e2c7f151c88a
    }

    @GetMapping("/productos/nuevo")
    public String formNuevo(Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        model.addAttribute("producto", new Producto());
        model.addAttribute("esNuevo", true);
        return "productos/producto-form";
    }

    @GetMapping("/productos/editar/{id}")
    public String formEditar(@PathVariable Integer id, Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        model.addAttribute("producto", productoService.obtenerPorId(id));
        model.addAttribute("esNuevo", false);
        return "productos/producto-form";
    }

    @PostMapping("/productos")
    public String crear(@ModelAttribute Producto producto,
                         @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
                         @RequestParam(defaultValue = "0") Integer stockInicial,
                         @RequestParam(defaultValue = "0") Integer stockMinimo) throws IOException {
        if (imagenFile != null && !imagenFile.isEmpty()) {
            producto.setImagen(imagenFile.getBytes());
        }
        productoService.crearProducto(producto, stockInicial, stockMinimo);
        return "redirect:/productos?exito=" + java.net.URLEncoder.encode("Producto creado con exito.",
                java.nio.charset.StandardCharsets.UTF_8);
    }

    @PostMapping("/productos/actualizar/{id}")
    public String actualizar(@PathVariable Integer id,
                              @ModelAttribute Producto producto,
                              @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile) throws IOException {
        if (imagenFile != null && !imagenFile.isEmpty()) {
            producto.setImagen(imagenFile.getBytes());
        }
        productoService.actualizarProducto(id, producto);
        return "redirect:/productos?exito=" + java.net.URLEncoder.encode("Producto actualizado con exito.",
                java.nio.charset.StandardCharsets.UTF_8);
    }

    @PostMapping("/productos/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        productoService.eliminarProducto(id);
        return "redirect:/productos?exito=" + java.net.URLEncoder.encode("Producto desactivado con exito.",
                java.nio.charset.StandardCharsets.UTF_8);
    }

    @PostMapping("/productos/reactivar/{id}")
    public String reactivar(@PathVariable Integer id) {
        productoService.reactivarProducto(id);
        return "redirect:/productos?exito=" + java.net.URLEncoder.encode("Producto reactivado con exito.",
                java.nio.charset.StandardCharsets.UTF_8);
    }

    private void agregarUsuarioAlModelo(Model model, Authentication authentication) {
        if (authentication == null) {
            return;
        }
        Usuario usuario = usuarioRepository.findByNombreUsuario(authentication.getName()).orElse(null);
        model.addAttribute("usuarioActual", usuario);
    }
}