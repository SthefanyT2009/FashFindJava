package com.fashfind.fashfind.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fashfind.fashfind.entity.Venta;

public interface VentaRepository extends JpaRepository<Venta, Integer> {

    @Query("SELECT v FROM Venta v JOIN FETCH v.usuario ORDER BY v.fechaVenta DESC, v.hora DESC")
    List<Venta> findAllConUsuario();

    @Query("SELECT DISTINCT v FROM Venta v JOIN FETCH v.usuario LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.idVenta = :id")
    Optional<Venta> findByIdConDetalles(Integer id);

    /**
     * Ventas con el estado indicado desde una fecha en adelante, usada para
     * calcular el total quincenal y la grafica de "Ventas - Ultimos 15 dias"
     * del dashboard con datos reales (las ventas desactivadas no se cuentan).
     */
    List<Venta> findByEstadoAndFechaVentaGreaterThanEqual(String estado, LocalDate desde);
}