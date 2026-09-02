package com.fashfind.fashfind.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Entidad que representa una venta de mostrador (venta directa en tienda,
 * distinta de Pedido que es la compra a domicilio del cliente).
 * El total (costo_total) y el sub_total de cada linea los recalculan los
 * triggers de la base de datos al insertar en Detalle_Venta, tal como
 * esta definido en el script Fash_Find.sql; por eso aqui tambien se
 * calculan en Java, para que la pantalla muestre el valor correcto
 * de inmediato sin depender de una segunda consulta.
 */
@Entity
@Table(name = "Venta")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Integer idVenta;

    @Column(name = "fecha_venta", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVenta;

    @Column(name = "hora", nullable = false)
    private LocalTime hora;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false)
    private MetodoPagoVenta metodoPago;

    @Column(name = "costo_total", nullable = false)
    private Integer costoTotal = 0;

    @Column(name = "pago_recibido", nullable = false)
    private Integer pagoRecibido;

    @Column(name = "cambio", nullable = false)
    private Integer cambio;

    @Column(name = "estado", nullable = false, length = 8)
    private String estado = "Activo";

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() {
    }

    // =========================================
    // GETTERS Y SETTERS
    // =========================================

    public Integer getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(Integer idVenta) {
        this.idVenta = idVenta;
    }

    public LocalDate getFechaVenta() {
        return fechaVenta;
    }

    public void setFechaVenta(LocalDate fechaVenta) {
        this.fechaVenta = fechaVenta;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public MetodoPagoVenta getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPagoVenta metodoPago) {
        this.metodoPago = metodoPago;
    }

    public Integer getCostoTotal() {
        return costoTotal;
    }

    public void setCostoTotal(Integer costoTotal) {
        this.costoTotal = costoTotal;
    }

    public Integer getPagoRecibido() {
        return pagoRecibido;
    }

    public void setPagoRecibido(Integer pagoRecibido) {
        this.pagoRecibido = pagoRecibido;
    }

    public Integer getCambio() {
        return cambio;
    }

    public void setCambio(Integer cambio) {
        this.cambio = cambio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }

    /**
     * Total de unidades vendidas en esta venta (suma de cantidades),
     * util para mostrar en la tabla/reporte sin iterar en la vista.
     */
    public int getTotalUnidades() {
        return detalles.stream().mapToInt(DetalleVenta::getCantidad).sum();
    }
}
