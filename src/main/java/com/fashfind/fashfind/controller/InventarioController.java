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

import com.fashfind.fashfind.entity.Inventario;
import com.fashfind.fashfind.entity.Usuario;
import com.fashfind.fashfind.repository.UsuarioRepository;
import com.fashfind.fashfind.service.InventarioService;
import com.fashfind.fashfind.service.ReporteInventarioService;

@Controller
public class InventarioController {

    private final InventarioService inventarioService;
    private final ReporteInventarioService reporteInventarioService;
    private final UsuarioRepository usuarioRepository;

    public InventarioController(InventarioService inventarioService,
                                 ReporteInventarioService reporteInventarioService,
                                 UsuarioRepository usuarioRepository) {
        this.inventarioService = inventarioService;
        this.reporteInventarioService = reporteInventarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/inventario")
    public String listar(Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);
        model.addAttribute("inventarios", inventarioService.listarInventario());
        return "inventario";
    }

    /**
     * Reporte de inventario: agrupa los mismos datos de /inventario en
     * indicadores rapidos (bajo stock, normal, sobrestock, valor total)
     * para una vista de solo lectura, pensada para imprimir o revisar.
     */
    @GetMapping("/inventario/reporte")
    public String reporte(Model model, Authentication authentication) {
        agregarUsuarioAlModelo(model, authentication);

        List<Inventario> inventarios = inventarioService.listarInventario();
        Estadisticas est = calcularEstadisticas(inventarios);

        model.addAttribute("inventarios", inventarios);
        model.addAttribute("totalReferencias", est.totalReferencias);
        model.addAttribute("bajoStock", est.bajoStock);
        model.addAttribute("normal", est.normal);
        model.addAttribute("sobrestock", est.sobrestock);
        model.addAttribute("unidadesTotales", est.unidadesTotales);
        model.addAttribute("valorTotal", est.valorTotal);
        return "inventario-reporte";
    }

    @GetMapping("/inventario/reporte/pdf")
    public ResponseEntity<byte[]> reportePdf() {
        List<Inventario> inventarios = inventarioService.listarInventario();
        Estadisticas est = calcularEstadisticas(inventarios);

        byte[] pdf = reporteInventarioService.generarPdf(inventarios, est.totalReferencias,
                est.unidadesTotales, est.bajoStock, est.normal, est.sobrestock, est.valorTotal);

        return archivoDescargable(pdf, MediaType.APPLICATION_PDF, "reporte-inventario.pdf");
    }

    @GetMapping("/inventario/reporte/excel")
    public ResponseEntity<byte[]> reporteExcel() {
        List<Inventario> inventarios = inventarioService.listarInventario();
        Estadisticas est = calcularEstadisticas(inventarios);

        byte[] excel = reporteInventarioService.generarExcel(inventarios, est.totalReferencias,
                est.unidadesTotales, est.bajoStock, est.normal, est.sobrestock, est.valorTotal);

        MediaType tipoExcel = MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return archivoDescargable(excel, tipoExcel, "reporte-inventario.xlsx");
    }

    @PostMapping("/inventario/actualizar/{id}")
    public String actualizar(@PathVariable Integer id,
                              @RequestParam Integer stockDisponible,
                              @RequestParam Integer stockMinimo) {
        inventarioService.actualizarStock(id, stockDisponible, stockMinimo);
        return "redirect:/inventario";
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

    private Estadisticas calcularEstadisticas(List<Inventario> inventarios) {
        long bajoStock = inventarios.stream().filter(i -> "Bajo Stock".equals(i.getNivelStock())).count();
        long sobrestock = inventarios.stream().filter(i -> "Sobrestock".equals(i.getNivelStock())).count();
        long normal = inventarios.size() - bajoStock - sobrestock;
        long unidadesTotales = inventarios.stream()
                .mapToLong(i -> i.getStockDisponible() != null ? i.getStockDisponible() : 0)
                .sum();
        long valorTotal = inventarios.stream()
                .mapToLong(i -> (long) (i.getStockDisponible() != null ? i.getStockDisponible() : 0)
                        * (i.getProducto().getPrecio() != null ? i.getProducto().getPrecio() : 0))
                .sum();
        return new Estadisticas(inventarios.size(), bajoStock, normal, sobrestock, unidadesTotales, valorTotal);
    }

    private void agregarUsuarioAlModelo(Model model, Authentication authentication) {
        if (authentication == null) {
            return;
        }
        Usuario usuario = usuarioRepository.findByNombreUsuario(authentication.getName()).orElse(null);
        model.addAttribute("usuarioActual", usuario);
    }

    /**
     * Contenedor simple de los indicadores del reporte, para no repetir
     * el calculo en cada endpoint (vista, PDF y Excel).
     */
    private static class Estadisticas {
        final long totalReferencias;
        final long bajoStock;
        final long normal;
        final long sobrestock;
        final long unidadesTotales;
        final long valorTotal;

        Estadisticas(long totalReferencias, long bajoStock, long normal, long sobrestock,
                     long unidadesTotales, long valorTotal) {
            this.totalReferencias = totalReferencias;
            this.bajoStock = bajoStock;
            this.normal = normal;
            this.sobrestock = sobrestock;
            this.unidadesTotales = unidadesTotales;
            this.valorTotal = valorTotal;
        }
    }
}