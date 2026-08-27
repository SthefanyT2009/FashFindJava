package com.fashfind.fashfind.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Inventario")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inventario")
    private Integer idInventario;

    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @Column(name = "estado", nullable = false, length = 8)
    private String estado = "Activo";

    @ManyToOne
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    public Inventario() {
    }

    /**
     * Nivel de stock calculado en base a stock_minimo, tal como se
     * clasificaba en la version React (Bajo Stock, Normal, Sobrestock).
     * Sobrestock = 3 veces o mas el minimo. Ajustable si el negocio
     * define otro criterio.
     */
    public String getNivelStock() {
        if (stockDisponible == null || stockMinimo == null) {
            return "Normal";
        }
        if (stockDisponible <= stockMinimo) {
            return "Bajo Stock";
        }
        if (stockDisponible >= stockMinimo * 3) {
            return "Sobrestock";
        }
        return "Normal";
    }

    // =========================================
    // GETTERS Y SETTERS
    // =========================================

    public Integer getIdInventario() {
        return idInventario;
    }

    public void setIdInventario(Integer idInventario) {
        this.idInventario = idInventario;
    }

    public Integer getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(Integer stockDisponible) {
        this.stockDisponible = stockDisponible;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }
}