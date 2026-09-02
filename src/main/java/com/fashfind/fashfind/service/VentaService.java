package com.fashfind.fashfind.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fashfind.fashfind.entity.DetalleVenta;
import com.fashfind.fashfind.entity.Inventario;
import com.fashfind.fashfind.entity.MetodoPagoVenta;
import com.fashfind.fashfind.entity.Producto;
import com.fashfind.fashfind.entity.Usuario;
import com.fashfind.fashfind.entity.Venta;
import com.fashfind.fashfind.repository.InventarioRepository;
import com.fashfind.fashfind.repository.ProductoRepository;
import com.fashfind.fashfind.repository.VentaRepository;

/**
 * Gestiona el ciclo de vida de una Venta de mostrador.
 *
 * El descuento de stock al registrar una venta y la devolucion de stock si
 * se elimina una linea lo hacen los triggers de la base de datos
 * (descontar_stock_venta / devolver_stock_venta / actualizar_stock_update_venta)
 * en cuanto Hibernate inserta o borra filas de Detalle_Venta; por eso crear()
 * y actualizar() nunca tocan Inventario directamente, solo agregan o quitan
 * lineas del detalle y dejan que el trigger ajuste el stock.
 *
 * En cambio, inactivar/reactivar una Venta NO inserta ni borra Detalle_Venta
 * (solo cambia el estado), asi que esos dos casos si devuelven o
 * vuelven a descontar el stock manualmente aqui.
 */
@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;

    public VentaService(VentaRepository ventaRepository, ProductoRepository productoRepository,
                         InventarioRepository inventarioRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
    }

    public List<Venta> listarVentas() {
        return ventaRepository.findAllConUsuario();
    }

    public Venta obtenerPorId(Integer id) {
        return ventaRepository.findByIdConDetalles(id)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada: " + id));
    }

    /**
     * Registra una venta nueva junto con sus lineas de detalle.
     * Valida que haya al menos un producto, que el stock alcance y que el
     * pago recibido cubra el total antes de guardar nada.
     */
    @Transactional
    public Venta crearVenta(MetodoPagoVenta metodoPago, Integer pagoRecibido,
                             List<Integer> productoIds, List<Integer> cantidades, Usuario usuarioActual) {
        Map<Producto, Integer> lineas = construirLineas(productoIds, cantidades);
        validarStockDisponible(lineas, null);

        Venta venta = new Venta();
        venta.setFechaVenta(LocalDate.now());
        venta.setHora(LocalTime.now());
        venta.setMetodoPago(metodoPago);
        venta.setEstado("Activo");
        venta.setUsuario(usuarioActual);

        int total = 0;
        for (Map.Entry<Producto, Integer> linea : lineas.entrySet()) {
            Producto producto = linea.getKey();
            Integer cantidad = linea.getValue();
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecio(producto.getPrecio());
            detalle.setSubTotal(cantidad * producto.getPrecio());
            detalle.setVenta(venta);
            venta.getDetalles().add(detalle);
            total += detalle.getSubTotal();
        }

        validarPago(pagoRecibido, total);
        venta.setCostoTotal(total);
        venta.setPagoRecibido(pagoRecibido);
        venta.setCambio(pagoRecibido - total);

        return ventaRepository.save(venta);
    }

    /**
     * Reemplaza las lineas de detalle y los datos de pago de una venta
     * existente. Al limpiar la coleccion de detalles con orphanRemoval y
     * agregar las nuevas lineas en la misma transaccion, los triggers de
     * borrado e insercion se compensan entre si y el stock queda correcto.
     */
    @Transactional
    public Venta actualizarVenta(Integer id, MetodoPagoVenta metodoPago, Integer pagoRecibido,
                                  List<Integer> productoIds, List<Integer> cantidades) {
        Venta venta = obtenerPorId(id);
        Map<Producto, Integer> lineas = construirLineas(productoIds, cantidades);

        // Cantidades que ya tenia esta misma venta, para no contarlas como "ocupadas"
        Map<Integer, Integer> cantidadesPrevias = new HashMap<>();
        for (DetalleVenta d : venta.getDetalles()) {
            cantidadesPrevias.merge(d.getProducto().getIdProducto(), d.getCantidad(), Integer::sum);
        }
        validarStockDisponible(lineas, cantidadesPrevias);

        venta.getDetalles().clear();
        int total = 0;
        for (Map.Entry<Producto, Integer> linea : lineas.entrySet()) {
            Producto producto = linea.getKey();
            Integer cantidad = linea.getValue();
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecio(producto.getPrecio());
            detalle.setSubTotal(cantidad * producto.getPrecio());
            detalle.setVenta(venta);
            venta.getDetalles().add(detalle);
            total += detalle.getSubTotal();
        }

        validarPago(pagoRecibido, total);
        venta.setMetodoPago(metodoPago);
        venta.setCostoTotal(total);
        venta.setPagoRecibido(pagoRecibido);
        venta.setCambio(pagoRecibido - total);

        return ventaRepository.save(venta);
    }

    /**
     * Baja logica de la venta: pone estado en Inactivo y devuelve al
     * inventario cada unidad vendida, ya que ningun trigger lo hace por
     * un simple cambio de estado.
     */
    @Transactional
    public void eliminarVenta(Integer id) {
        Venta venta = obtenerPorId(id);
        if (!"Activo".equals(venta.getEstado())) {
            return;
        }
        for (DetalleVenta detalle : venta.getDetalles()) {
            Inventario inventario = inventarioRepository.findByProducto_IdProducto(detalle.getProducto().getIdProducto())
                    .orElseThrow(() -> new IllegalStateException(
                            "El producto " + detalle.getProducto().getNombreProducto() + " no tiene inventario asociado"));
            inventario.setStockDisponible(inventario.getStockDisponible() + detalle.getCantidad());
            inventarioRepository.save(inventario);
        }
        venta.setEstado("Inactivo");
        ventaRepository.save(venta);
    }

    /**
     * Reactiva una venta inactivada, volviendo a descontar el stock que se
     * le habia devuelto. Si ya no hay unidades suficientes de algun
     * producto, no reactiva nada y avisa cual producto quedo corto.
     */
    @Transactional
    public void reactivarVenta(Integer id) {
        Venta venta = obtenerPorId(id);
        if ("Activo".equals(venta.getEstado())) {
            return;
        }
        Map<Integer, Inventario> inventariosAfectados = new HashMap<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            Inventario inventario = inventarioRepository.findByProducto_IdProducto(detalle.getProducto().getIdProducto())
                    .orElseThrow(() -> new IllegalStateException(
                            "El producto " + detalle.getProducto().getNombreProducto() + " no tiene inventario asociado"));
            int disponibleActual = inventariosAfectados.containsKey(inventario.getIdInventario())
                    ? inventariosAfectados.get(inventario.getIdInventario()).getStockDisponible()
                    : inventario.getStockDisponible();
            if (disponibleActual < detalle.getCantidad()) {
                throw new IllegalStateException(
                        "No hay stock suficiente de \"" + detalle.getProducto().getNombreProducto()
                                + "\" para reactivar esta venta.");
            }
            inventario.setStockDisponible(disponibleActual - detalle.getCantidad());
            inventariosAfectados.put(inventario.getIdInventario(), inventario);
        }
        inventariosAfectados.values().forEach(inventarioRepository::save);
        venta.setEstado("Activo");
        ventaRepository.save(venta);
    }

    // =========================================
    // Helpers privados
    // =========================================

    /**
     * Agrupa productoIds/cantidades en un mapa Producto -> cantidad total,
     * sumando si el mismo producto aparece en mas de una fila del formulario.
     */
    private Map<Producto, Integer> construirLineas(List<Integer> productoIds, List<Integer> cantidades) {
        if (productoIds == null || cantidades == null || productoIds.isEmpty()
                || productoIds.size() != cantidades.size()) {
            throw new IllegalArgumentException("Debes agregar al menos un producto a la venta.");
        }
        Map<Producto, Integer> lineas = new java.util.LinkedHashMap<>();
        for (int i = 0; i < productoIds.size(); i++) {
            Integer idProducto = productoIds.get(i);
            Integer cantidad = cantidades.get(i);
            if (idProducto == null || cantidad == null || cantidad <= 0) {
                continue;
            }
            Producto producto = productoRepository.findById(idProducto)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + idProducto));
            lineas.merge(producto, cantidad, Integer::sum);
        }
        if (lineas.isEmpty()) {
            throw new IllegalArgumentException("Debes agregar al menos un producto valido a la venta.");
        }
        return lineas;
    }

    /**
     * Verifica que haya stock suficiente para cada linea. Si se pasa un
     * mapa de cantidadesPrevias (caso actualizar), esas unidades se suman
     * de vuelta al disponible porque se van a liberar antes de descontar
     * las nuevas.
     */
    private void validarStockDisponible(Map<Producto, Integer> lineas, Map<Integer, Integer> cantidadesPrevias) {
        for (Map.Entry<Producto, Integer> linea : lineas.entrySet()) {
            Producto producto = linea.getKey();
            if (!"Activo".equals(producto.getEstado())) {
                throw new IllegalArgumentException("El producto \"" + producto.getNombreProducto() + "\" ya no esta activo.");
            }
            Inventario inventario = inventarioRepository.findByProducto_IdProducto(producto.getIdProducto())
                    .orElseThrow(() -> new IllegalStateException(
                            "El producto " + producto.getNombreProducto() + " no tiene inventario asociado"));
            int disponible = inventario.getStockDisponible();
            if (cantidadesPrevias != null) {
                disponible += cantidadesPrevias.getOrDefault(producto.getIdProducto(), 0);
            }
            if (linea.getValue() > disponible) {
                throw new IllegalArgumentException("Stock insuficiente de \"" + producto.getNombreProducto()
                        + "\". Disponible: " + disponible + ", solicitado: " + linea.getValue() + ".");
            }
        }
    }

    private void validarPago(Integer pagoRecibido, int total) {
        if (pagoRecibido == null || pagoRecibido < total) {
            throw new IllegalArgumentException("El pago recibido debe ser mayor o igual al total de la venta.");
        }
    }

    /**
     * Lista de productos activos con stock disponible, para poblar el
     * selector del formulario de venta (no tiene sentido ofrecer productos
     * agotados o inactivos).
     */
    public List<ProductoConStock> listarProductosDisponibles() {
        List<ProductoConStock> resultado = new ArrayList<>();
        for (Inventario inv : inventarioRepository.findAllConProducto()) {
            if ("Activo".equals(inv.getEstado()) && "Activo".equals(inv.getProducto().getEstado())
                    && inv.getStockDisponible() > 0) {
                resultado.add(new ProductoConStock(inv.getProducto(), inv.getStockDisponible()));
            }
        }
        return resultado;
    }

    /**
     * Igual que listarProductosDisponibles(), pero para el formulario de
     * edicion: a los productos con stock libre les suma las unidades que
     * ya tenia reservadas esta misma venta, para que sigan apareciendo
     * como opcion (con el stock "efectivo" correcto) aunque su stock
     * libre actual sea 0.
     */
    public List<ProductoConStock> listarProductosDisponiblesParaEditar(Venta venta) {
        Map<Integer, Integer> cantidadesPrevias = new HashMap<>();
        for (DetalleVenta d : venta.getDetalles()) {
            cantidadesPrevias.merge(d.getProducto().getIdProducto(), d.getCantidad(), Integer::sum);
        }

        List<ProductoConStock> resultado = new ArrayList<>();
        for (Inventario inv : inventarioRepository.findAllConProducto()) {
            if (!"Activo".equals(inv.getProducto().getEstado())) {
                continue;
            }
            int extra = cantidadesPrevias.getOrDefault(inv.getProducto().getIdProducto(), 0);
            int stockEfectivo = inv.getStockDisponible() + extra;
            if ("Activo".equals(inv.getEstado()) && stockEfectivo > 0) {
                resultado.add(new ProductoConStock(inv.getProducto(), stockEfectivo));
            }
        }
        return resultado;
    }

    /** Envoltorio simple para pasarle a la vista el producto junto a su stock actual. */
    public static class ProductoConStock {
        private final Producto producto;
        private final Integer stockDisponible;

        public ProductoConStock(Producto producto, Integer stockDisponible) {
            this.producto = producto;
            this.stockDisponible = stockDisponible;
        }

        public Producto getProducto() {
            return producto;
        }

        public Integer getStockDisponible() {
            return stockDisponible;
        }
    }
}