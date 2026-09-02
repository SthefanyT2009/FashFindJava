package com.fashfind.fashfind.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fashfind.fashfind.entity.Cargo;
import com.fashfind.fashfind.entity.MetodoPagoVenta;
import com.fashfind.fashfind.entity.Usuario;
import com.fashfind.fashfind.entity.Venta;
import com.fashfind.fashfind.repository.UsuarioRepository;
import com.fashfind.fashfind.service.ReporteVentaService;
import com.fashfind.fashfind.service.VentaService;

@Controller
public class VentaController {

    private final VentaService ventaService;
    private final ReporteVentaService reporteVentaService;
    private final UsuarioRepository usuarioRepository;

    public VentaController(VentaService ventaService, ReporteVentaService reporteVentaService,
                            UsuarioRepository usuarioRepository) {
        this.ventaService = ventaService;
        this.reporteVentaService = reporteVentaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/ventas")
    public String listar(@RequestParam(required = false) String error, @RequestParam(required = false) String exito,
                          Model model, Authentication authentication) {
        Usuario usuarioActual = agregarUsuarioAlModelo(model, authentication);

        // El Administrador ve todas las ventas; el Vendedor solo las que el mismo registro.
        List<Venta> ventas = (usuarioActual != null && usuarioActual.getCargo() == Cargo.Vendedor)
                ? ventaService.listarVentasDeUsuario(usuarioActual.getIdUsuario())
                : ventaService.listarVentas();
        model.addAttribute("ventas", ventas);

        if (error != null) {
            model.addAttribute("error", error);
        }
        if (exito != null) {
            model.addAttribute("exito", exito);
        }
        return "ventas/ventas";
    }

    @GetMapping("/ventas/{id}")
    public String verDetalle(@PathVariable Integer id, Model model, Authentication authentication) {
        Usuario usuarioActual = agregarUsuarioAlModelo(model, authentication);
        Venta venta = ventaService.obtenerPorId(id);

        // El Vendedor solo puede ver el detalle de sus propias ventas, aunque
        // escriba el numero de otra venta directamente en la URL.
        boolean esVendedor = usuarioActual != null && usuarioActual.getCargo() == Cargo.Vendedor;
        boolean noEsSuya = venta.getUsuario() == null
                || !venta.getUsuario().getIdUsuario().equals(usuarioActual.getIdUsuario());
        if (esVendedor && noEsSuya) {
            return "redirect:/ventas?error=" + encodar("No tienes permiso para ver esa venta.");
        }

        model.addAttribute("venta", venta);
        return "ventas/venta-detalle";
    }

    @GetMapping("/ventas/nueva")
    public String formNueva(@RequestParam(required = false) String error, Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        model.addAttribute("venta", new Venta());
        model.addAttribute("productosDisponibles", ventaService.listarProductosDisponibles());
        model.addAttribute("lineasIniciales", java.util.Collections.emptyList());
        model.addAttribute("esNueva", true);
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "ventas/venta-form";
    }

    @GetMapping("/ventas/editar/{id}")
    public String formEditar(@PathVariable Integer id, @RequestParam(required = false) String error,
                              Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        Venta venta = ventaService.obtenerPorId(id);
        model.addAttribute("venta", venta);
        model.addAttribute("productosDisponibles", ventaService.listarProductosDisponiblesParaEditar(venta));
        // DTO simple (sin referencia circular a la venta) para inyectar en el JS de la vista
        List<java.util.Map<String, Object>> lineas = venta.getDetalles().stream()
                .map(d -> java.util.Map.<String, Object>of(
                        "idProducto", d.getProducto().getIdProducto(),
                        "cantidad", d.getCantidad()))
                .toList();
        model.addAttribute("lineasIniciales", lineas);
        model.addAttribute("esNueva", false);
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "ventas/venta-form";
    }

    @PostMapping("/ventas")
    public String crear(@RequestParam MetodoPagoVenta metodoPago,
                         @RequestParam Integer pagoRecibido,
                         @RequestParam(value = "productoId", required = false) List<Integer> productoId,
                         @RequestParam(value = "cantidad", required = false) List<Integer> cantidad,
                         Authentication authentication) {
        try {
            Usuario usuarioActual = usuarioRepository.findByNombreUsuario(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));
            ventaService.crearVenta(metodoPago, pagoRecibido, productoId, cantidad, usuarioActual);
            return "redirect:/ventas?exito=" + encodar("Venta registrada con exito.");
        } catch (RuntimeException e) {
            return "redirect:/ventas/nueva?error=" + encodar(e.getMessage());
        }
    }

    @PostMapping("/ventas/actualizar/{id}")
    public String actualizar(@PathVariable Integer id,
                              @RequestParam MetodoPagoVenta metodoPago,
                              @RequestParam Integer pagoRecibido,
                              @RequestParam(value = "productoId", required = false) List<Integer> productoId,
                              @RequestParam(value = "cantidad", required = false) List<Integer> cantidad) {
        try {
            ventaService.actualizarVenta(id, metodoPago, pagoRecibido, productoId, cantidad);
            return "redirect:/ventas?exito=" + encodar("Venta actualizada con exito.");
        } catch (RuntimeException e) {
            return "redirect:/ventas/editar/" + id + "?error=" + encodar(e.getMessage());
        }
    }

    @PostMapping("/ventas/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        try {
            ventaService.eliminarVenta(id);
        } catch (RuntimeException e) {
            return "redirect:/ventas?error=" + encodar(e.getMessage());
        }
        return "redirect:/ventas?exito=" + encodar("Venta desactivada con exito.");
    }

    @PostMapping("/ventas/reactivar/{id}")
    public String reactivar(@PathVariable Integer id) {
        try {
            ventaService.reactivarVenta(id);
        } catch (RuntimeException e) {
            return "redirect:/ventas?error=" + encodar(e.getMessage());
        }
        return "redirect:/ventas?exito=" + encodar("Venta reactivada con exito.");
    }

    @GetMapping("/ventas/reporte")
    public String reporte(Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        List<Venta> ventas = ventaService.listarVentas();
        Estadisticas est = calcularEstadisticas(ventas);

        model.addAttribute("ventas", ventas);
        model.addAttribute("totalVentas", est.totalVentas);
        model.addAttribute("unidadesVendidas", est.unidadesVendidas);
        model.addAttribute("ingresosTotales", est.ingresosTotales);
        model.addAttribute("ventasEfectivo", est.ventasEfectivo);
        model.addAttribute("ventasTransferencia", est.ventasTransferencia);
        model.addAttribute("ticketPromedio", est.ticketPromedio);
        return "ventas/venta-reporte";
    }

    @GetMapping("/ventas/reporte/pdf")
    public ResponseEntity<byte[]> reportePdf() {
        List<Venta> ventasActivas = soloActivas(ventaService.listarVentas());
        Estadisticas est = calcularEstadisticas(ventasActivas);

        byte[] pdf = reporteVentaService.generarPdf(ventasActivas, est.totalVentas, est.unidadesVendidas,
                est.ingresosTotales, est.ventasEfectivo, est.ventasTransferencia, est.ticketPromedio);

        return archivoDescargable(pdf, MediaType.APPLICATION_PDF, "reporte-ventas.pdf");
    }

    @GetMapping("/ventas/reporte/excel")
    public ResponseEntity<byte[]> reporteExcel() {
        List<Venta> ventasActivas = soloActivas(ventaService.listarVentas());
        Estadisticas est = calcularEstadisticas(ventasActivas);

        byte[] excel = reporteVentaService.generarExcel(ventasActivas, est.totalVentas, est.unidadesVendidas,
                est.ingresosTotales, est.ventasEfectivo, est.ventasTransferencia, est.ticketPromedio);

        MediaType tipoExcel = MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return archivoDescargable(excel, tipoExcel, "reporte-ventas.xlsx");
    }

    /**
     * Los reportes descargables (PDF/Excel) solo deben incluir ventas
     * activas, a diferencia de la vista en pantalla que muestra todas.
     */
    private List<Venta> soloActivas(List<Venta> ventas) {
        return ventas.stream()
                .filter(v -> "Activo".equalsIgnoreCase(v.getEstado()))
                .toList();
    }

    private ResponseEntity<byte[]> archivoDescargable(byte[] contenido, MediaType tipo, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(tipo);
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.attachment()
                        .filename(nombreArchivo, StandardCharsets.UTF_8)
                        .build());
        return new ResponseEntity<>(contenido, headers, org.springframework.http.HttpStatus.OK);
    }

    private Estadisticas calcularEstadisticas(List<Venta> ventas) {
        List<Venta> activas = ventas.stream().filter(v -> "Activo".equals(v.getEstado())).toList();
        long totalVentas = activas.size();
        long unidadesVendidas = activas.stream().mapToLong(Venta::getTotalUnidades).sum();
        long ingresosTotales = activas.stream().mapToLong(v -> v.getCostoTotal() != null ? v.getCostoTotal() : 0).sum();
        long ventasEfectivo = activas.stream().filter(v -> v.getMetodoPago() == MetodoPagoVenta.Efectivo).count();
        long ventasTransferencia = activas.stream().filter(v -> v.getMetodoPago() == MetodoPagoVenta.Transferencia).count();
        long ticketPromedio = totalVentas > 0 ? ingresosTotales / totalVentas : 0;
        return new Estadisticas(totalVentas, unidadesVendidas, ingresosTotales, ventasEfectivo, ventasTransferencia, ticketPromedio);
    }

    private String encodar(String mensaje) {
        if (mensaje == null) {
            mensaje = "Ocurrio un error inesperado.";
        }
        return java.net.URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }

    private Usuario agregarUsuarioAlModelo(Model model, Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Usuario usuario = usuarioRepository.findByNombreUsuario(authentication.getName()).orElse(null);
        model.addAttribute("usuarioActual", usuario);
        return usuario;
    }

    private static class Estadisticas {
        final long totalVentas;
        final long unidadesVendidas;
        final long ingresosTotales;
        final long ventasEfectivo;
        final long ventasTransferencia;
        final long ticketPromedio;

        Estadisticas(long totalVentas, long unidadesVendidas, long ingresosTotales,
                     long ventasEfectivo, long ventasTransferencia, long ticketPromedio) {
            this.totalVentas = totalVentas;
            this.unidadesVendidas = unidadesVendidas;
            this.ingresosTotales = ingresosTotales;
            this.ventasEfectivo = ventasEfectivo;
            this.ventasTransferencia = ventasTransferencia;
            this.ticketPromedio = ticketPromedio;
        }
    }
}