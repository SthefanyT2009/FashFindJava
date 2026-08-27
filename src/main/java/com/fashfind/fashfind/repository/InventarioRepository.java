package com.fashfind.fashfind.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fashfind.fashfind.entity.Inventario;

public interface InventarioRepository extends JpaRepository<Inventario, Integer> {

    @Query("SELECT i FROM Inventario i JOIN FETCH i.producto p ORDER BY p.nombreProducto")
    List<Inventario> findAllConProducto();

    Optional<Inventario> findByProducto_IdProducto(Integer idProducto);
}