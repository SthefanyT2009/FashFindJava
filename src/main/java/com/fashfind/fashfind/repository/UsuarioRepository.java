package com.fashfind.fashfind.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fashfind.fashfind.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCc(Long cc);

    boolean existsByCorreo(String correo);
}
