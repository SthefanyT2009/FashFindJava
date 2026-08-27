package com.fashfind.fashfind.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fashfind.fashfind.entity.Inventario;
import com.fashfind.fashfind.entity.Producto;
import com.fashfind.fashfind.repository.InventarioRepository;
import com.fashfind.fashfind.repository.ProductoRepository;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;

    public ProductoService(ProductoRepository productoRepository, InventarioRepository inventarioRepository) {
        this.productoRepository = productoRepository;
        this.inventarioRepository = inventarioRepository;
    }

    public List<Producto> listarProductos() {
        return productoRepository.findAll(Sort.by(Sort.Direction.DESC, "idProducto"));
    }

    public Producto obtenerPorId(Integer id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
    }

    /**
     * Crea el producto y, en la misma transaccion, su registro de Inventario
     * asociado. Asi nunca queda un producto sin inventario.
     */
    @Transactional
    public Producto crearProducto(Producto producto, Integer stockInicial, Integer stockMinimo) {
        producto.setEstado("Activo");
        Producto guardado = productoRepository.save(producto);

        Inventario inventario = new Inventario();
        inventario.setProducto(guardado);
        inventario.setStockDisponible(stockInicial != null ? stockInicial : 0);
        inventario.setStockMinimo(stockMinimo != null ? stockMinimo : 0);
        inventario.setEstado("Activo");
        inventarioRepository.save(inventario);

        return guardado;
    }

    public void actualizarProducto(Integer id, Producto datos) {
        Producto producto = obtenerPorId(id);
        producto.setNombreProducto(datos.getNombreProducto());
        producto.setDescripcion(datos.getDescripcion());
        producto.setTalla(datos.getTalla());
        producto.setColor(datos.getColor());
        producto.setPrecio(datos.getPrecio());
        if (datos.getImagen() != null && datos.getImagen().length > 0) {
            producto.setImagen(datos.getImagen());
        }
        productoRepository.save(producto);
    }

    /**
     * Baja logica: pone el producto Y su inventario en "Inactivo",
     * igual que hacia api/productos.php con la accion "eliminar".
     */
    @Transactional
    public void eliminarProducto(Integer id) {
        Producto producto = obtenerPorId(id);
        producto.setEstado("Inactivo");
        productoRepository.save(producto);

        inventarioRepository.findByProducto_IdProducto(id).ifPresent(inv -> {
            inv.setEstado("Inactivo");
            inventarioRepository.save(inv);
        });
    }

    @Transactional
    public void reactivarProducto(Integer id) {
        Producto producto = obtenerPorId(id);
        producto.setEstado("Activo");
        productoRepository.save(producto);

        inventarioRepository.findByProducto_IdProducto(id).ifPresent(inv -> {
            inv.setEstado("Activo");
            inventarioRepository.save(inv);
        });
    }
}