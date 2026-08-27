package com.fashfind.fashfind.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "Producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @Lob
    @Column(name = "imagen")
    private byte[] imagen;

    @Column(name = "nombre_producto", nullable = false, length = 100)
    private String nombreProducto;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "talla", nullable = false, length = 100)
    private String talla;

    @Column(name = "color", nullable = false, length = 100)
    private String color;

    @Column(name = "precio", nullable = false)
    private Integer precio;

    @Column(name = "estado", nullable = false, length = 8)
    private String estado = "Activo";

    public Producto() {
    }

    // =========================================
    // GETTERS Y SETTERS
    // =========================================

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public byte[] getImagen() {
        return imagen;
    }

    public void setImagen(byte[] imagen) {
        this.imagen = imagen;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTalla() {
        return talla;
    }

    public void setTalla(String talla) {
        this.talla = talla;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getPrecio() {
        return precio;
    }

    public void setPrecio(Integer precio) {
        this.precio = precio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Helper para mostrar la imagen en el HTML sin tocar el mapeo JPA
     * (no es un campo persistente, es solo una conversion de lectura).
     */
    public String getImagenBase64() {
        return imagen != null && imagen.length > 0
                ? java.util.Base64.getEncoder().encodeToString(imagen)
                : null;
    }
}