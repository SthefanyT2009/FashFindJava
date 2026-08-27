package com.fashfind.fashfind.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.fashfind.fashfind.entity.Usuario;
import com.fashfind.fashfind.repository.UsuarioRepository;

/**
 * Servicio que Spring Security utiliza para autenticar contra la tabla Usuario.
 * A diferencia de OdontoClinic (roles en tabla aparte), FashFind guarda
 * el rol del usuario en un unico campo "cargo" (Administrador, Vendedor,
 * Domiciliario, Cliente), y el login se hace por nombre_usuario, no por email.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con el nombre de usuario: " + nombreUsuario));

        boolean habilitado = "Activo".equalsIgnoreCase(usuario.getEstado());

        return new User(
                usuario.getNombreUsuario(),
                usuario.getContrasena(),
                habilitado, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getCargo().name().toUpperCase()))
        );
    }
}
