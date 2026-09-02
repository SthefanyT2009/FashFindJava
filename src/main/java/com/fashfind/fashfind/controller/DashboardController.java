package com.fashfind.fashfind.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fashfind.fashfind.entity.Cargo;
import com.fashfind.fashfind.entity.Usuario;
import com.fashfind.fashfind.entity.Venta;
import com.fashfind.fashfind.repository.ProductoRepository;
import com.fashfind.fashfind.repository.UsuarioRepository;
import com.fashfind.fashfind.repository.VentaRepository;

@Controller
public class DashboardController {

    /** Ventana de la grafica y de la tarjeta "Ventas Quincenales": hoy y los 14 dias anteriores. */
    private static final int DIAS_QUINCENA = 15;

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;

    public DashboardController(UsuarioRepository usuarioRepository, ProductoRepository productoRepository,
                                VentaRepository ventaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.ventaRepository = ventaRepository;
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
        return "interfaces/cliente-dashboard";
    }

    @GetMapping("/vendedor-dashboard")
    public String vendedorDashboard(Model model, Authentication authentication) {
        Usuario usuario = agregarUsuarioAlModelo(model, authentication);
        if (usuario != null) {
            agregarEstadisticasVendedorAlModelo(model, usuario);
        }
        return "interfaces/vendedor-dashboard";
    }

    @GetMapping("/domiciliario-dashboard")
    public String domiciliarioDashboard(Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        return "interfaces/domiciliario-dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        agregarEstadisticasAlModelo(model);
        return "interfaces/admin-dashboard";
    }

    private Usuario agregarUsuarioAlModelo(Model model, Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Usuario usuario = usuarioRepository.findByNombreUsuario(authentication.getName()).orElse(null);
        model.addAttribute("usuarioActual", usuario);
        return usuario;
    }

    /**
     * Calcula las cifras de la pagina principal del Vendedor: cuantas ventas
     * ha registrado el, cuanto suman sus ventas de los ultimos 15 dias (con
     * la misma grafica que usa el Administrador, pero filtrada a el) y
     * cuantos productos siguen activos en el catalogo (dato compartido,
     * ya que el catalogo no le pertenece a un vendedor en particular).
     */
    private void agregarEstadisticasVendedorAlModelo(Model model, Usuario usuario) {
        // --- Ventas realizadas por el (activas) ---
        long ventasRealizadas = ventaRepository.countByEstadoAndUsuario_IdUsuario("Activo", usuario.getIdUsuario());
        model.addAttribute("ventasRealizadas", ventasRealizadas);

        // --- Productos activos (catalogo general) ---
        model.addAttribute("productosActivos", productoRepository.countByEstado("Activo"));

        // --- Ventas quincenales de el (ultimos 15 dias, solo ventas activas) ---
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.minusDays(DIAS_QUINCENA - 1L);
        List<Venta> ventasQuincena = ventaRepository.findByEstadoAndFechaVentaGreaterThanEqualAndUsuario_IdUsuario(
                "Activo", desde, usuario.getIdUsuario());

        long ventasQuincenales = ventasQuincena.stream()
                .mapToLong(v -> v.getCostoTotal() != null ? v.getCostoTotal() : 0)
                .sum();
        model.addAttribute("ventasQuincenales", ventasQuincenales);

        // --- Serie diaria para la grafica "Ventas - Ultimos 15 dias" (solo de el) ---
        Map<LocalDate, Long> totalPorDia = new LinkedHashMap<>();
        for (int i = 0; i < DIAS_QUINCENA; i++) {
            totalPorDia.put(desde.plusDays(i), 0L);
        }
        for (Venta venta : ventasQuincena) {
            totalPorDia.merge(venta.getFechaVenta(),
                    venta.getCostoTotal() != null ? venta.getCostoTotal().longValue() : 0L, Long::sum);
        }

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM");
        List<String> etiquetas = new ArrayList<>();
        List<Long> valores = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entrada : totalPorDia.entrySet()) {
            etiquetas.add(entrada.getKey().format(formato));
            valores.add(entrada.getValue());
        }
        model.addAttribute("ventasEtiquetas", etiquetas);
        model.addAttribute("ventasValores", valores);
    }

    /**
     * Calcula, con datos reales de la base de datos, todas las cifras que
     * muestra la pagina principal del administrador. Cada conteo respeta el
     * campo "estado" de cada entidad: lo que un usuario, cliente, producto o
     * venta desactiva deja de sumar en su tarjeta de "activos" y pasa a la
     * de "inactivos" correspondiente, en vez de seguir contando como si
     * siguiera activo.
     */
    private void agregarEstadisticasAlModelo(Model model) {
        // --- Usuarios internos (Administrador / Vendedor / Domiciliario) ---
        model.addAttribute("usuariosActivos", usuarioRepository.countByCargoNotAndEstado(Cargo.Cliente, "Activo"));
        model.addAttribute("usuariosInactivos", usuarioRepository.countByCargoNotAndEstado(Cargo.Cliente, "Inactivo"));

        // --- Clientes (cargo = Cliente) ---
        model.addAttribute("clientesRegistrados", usuarioRepository.countByCargoAndEstado(Cargo.Cliente, "Activo"));
        model.addAttribute("clientesInactivos", usuarioRepository.countByCargoAndEstado(Cargo.Cliente, "Inactivo"));

        // --- Productos ---
        model.addAttribute("productosActivos", productoRepository.countByEstado("Activo"));
        model.addAttribute("productosInactivos", productoRepository.countByEstado("Inactivo"));

        // --- Ventas quincenales (ultimos 15 dias, solo ventas activas) ---
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.minusDays(DIAS_QUINCENA - 1L);
        List<Venta> ventasQuincena = ventaRepository.findByEstadoAndFechaVentaGreaterThanEqual("Activo", desde);

        long ventasQuincenales = ventasQuincena.stream()
                .mapToLong(v -> v.getCostoTotal() != null ? v.getCostoTotal() : 0)
                .sum();
        model.addAttribute("ventasQuincenales", ventasQuincenales);

        // --- Serie diaria para la grafica "Ventas - Ultimos 15 dias" ---
        Map<LocalDate, Long> totalPorDia = new LinkedHashMap<>();
        for (int i = 0; i < DIAS_QUINCENA; i++) {
            totalPorDia.put(desde.plusDays(i), 0L);
        }
        for (Venta venta : ventasQuincena) {
            totalPorDia.merge(venta.getFechaVenta(),
                    venta.getCostoTotal() != null ? venta.getCostoTotal().longValue() : 0L, Long::sum);
        }

        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM");
        List<String> etiquetas = new ArrayList<>();
        List<Long> valores = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entrada : totalPorDia.entrySet()) {
            etiquetas.add(entrada.getKey().format(formato));
            valores.add(entrada.getValue());
        }
        model.addAttribute("ventasEtiquetas", etiquetas);
        model.addAttribute("ventasValores", valores);
    }
}