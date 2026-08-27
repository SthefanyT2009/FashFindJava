package com.fashfind.fashfind.entity;

/**
 * Rol del usuario dentro del sistema. Corresponde al campo ENUM "cargo"
 * de la tabla Usuario. Se usa tanto para autorizacion (Spring Security
 * antepone "ROLE_" al nombre) como para la redireccion tras el login.
 */
public enum Cargo {
    Administrador,
    Vendedor,
    Domiciliario,
    Cliente
}
