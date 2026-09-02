package com.fashfind.fashfind.entity;

/**
 * Metodo de pago de una Venta (mostrador). Corresponde al ENUM
 * "metodo_pago" de la tabla Venta: Efectivo o Transferencia.
 * No confundir con el metodo de pago de Pedido (Nequi, Daviplata, etc.).
 */
public enum MetodoPagoVenta {
    Efectivo,
    Transferencia
}
