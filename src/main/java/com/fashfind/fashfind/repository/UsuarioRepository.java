package com.fashfind.fashfind.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fashfind.fashfind.entity.Cargo;
import com.fashfind.fashfind.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCc(Long cc);

    boolean existsByCorreo(String correo);

    /**
     * Cuenta usuarios internos (Administrador/Vendedor/Domiciliario, es decir
     * distinto de Cliente) filtrados por estado. Sirve para las tarjetas
     * "Usuarios Activos" / "Usuarios Inactivos" del dashboard.
     */
    long countByCargoNotAndEstado(Cargo cargo, String estado);

    /**
     * Cuenta usuarios con cargo Cliente filtrados por estado. Sirve para las
     * tarjetas "Clientes Registrados" / "Clientes Inactivos" del dashboard.
     */
    long countByCargoAndEstado(Cargo cargo, String estado);

    // ---- Validaciones de unicidad al editar (excluyen el propio registro) ----
    boolean existsByNombreUsuarioAndIdUsuarioNot(String nombreUsuario, Integer idUsuario);

    boolean existsByCorreoAndIdUsuarioNot(String correo, Integer idUsuario);

    boolean existsByCcAndIdUsuarioNot(Long cc, Integer idUsuario);
}