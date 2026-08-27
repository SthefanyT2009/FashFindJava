package com.fashfind.fashfind.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fashfind.fashfind.entity.Inventario;
import com.fashfind.fashfind.repository.InventarioRepository;

@Service
public class InventarioService {

    private final InventarioRepository inventarioRepository;

    public InventarioService(InventarioRepository inventarioRepository) {
        this.inventarioRepository = inventarioRepository;
    }

    public List<Inventario> listarInventario() {
        return inventarioRepository.findAllConProducto();
    }

    public void actualizarStock(Integer idInventario, Integer nuevoStockDisponible, Integer nuevoStockMinimo) {
        Inventario inventario = inventarioRepository.findById(idInventario)
                .orElseThrow(() -> new IllegalArgumentException("Inventario no encontrado: " + idInventario));

        if (nuevoStockDisponible != null && nuevoStockDisponible >= 0) {
            inventario.setStockDisponible(nuevoStockDisponible);
        }
        if (nuevoStockMinimo != null && nuevoStockMinimo >= 0) {
            inventario.setStockMinimo(nuevoStockMinimo);
        }
        inventarioRepository.save(inventario);
    }
}