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

    /**
     * Igual que findAllConUsuario(), pero solo las ventas registradas por un
     * usuario especifico. Se usa para que la tabla de "Gestion de Ventas"
     * del Vendedor solo muestre las ventas que el mismo realizo, mientras
     * que el Administrador sigue viendo findAllConUsuario() (todas).
     */
    @Query("SELECT v FROM Venta v JOIN FETCH v.usuario WHERE v.usuario.idUsuario = :idUsuario "
            + "ORDER BY v.fechaVenta DESC, v.hora DESC")
    List<Venta> findByUsuarioIdConUsuario(Integer idUsuario);

    @Query("SELECT DISTINCT v FROM Venta v JOIN FETCH v.usuario LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.idVenta = :id")
    Optional<Venta> findByIdConDetalles(Integer id);

    /**
     * Ventas con el estado indicado desde una fecha en adelante, usada para
     * calcular el total quincenal y la grafica de "Ventas - Ultimos 15 dias"
     * del dashboard con datos reales (las ventas desactivadas no se cuentan).
     */
    List<Venta> findByEstadoAndFechaVentaGreaterThanEqual(String estado, LocalDate desde);

    /**
     * Igual que la anterior, pero filtrada por el vendedor que la registro.
     * Se usa para la grafica y la tarjeta "Ventas Quincenales" del panel del
     * Vendedor, que solo debe mostrar sus propias ventas.
     */
    List<Venta> findByEstadoAndFechaVentaGreaterThanEqualAndUsuario_IdUsuario(String estado, LocalDate desde,
                                                                               Integer idUsuario);

    /**
     * Cuenta las ventas activas registradas por un vendedor especifico,
     * usada en la tarjeta "Ventas Realizadas" de su panel principal.
     */
    long countByEstadoAndUsuario_IdUsuario(String estado, Integer idUsuario);
}