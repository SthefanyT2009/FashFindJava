package com.fashfind.fashfind.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * Componente de Spring Security encargado de redirigir al usuario
 * a su panel correspondiente según su rol (cargo) al iniciar sesión con éxito.
 * Roles esperados: ROLE_ADMINISTRADOR, ROLE_VENDEDOR, ROLE_DOMICILIARIO, ROLE_CLIENTE.
 */
@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

                String redirectUrl = "/inicio"; // Ruta por defecto

        for (GrantedAuthority authority : authorities) {
            String rol = authority.getAuthority();

            if (rol.equals("ROLE_ADMINISTRADOR")) {
                redirectUrl = "/admin/dashboard";
                break;
            } else if (rol.equals("ROLE_VENDEDOR")) {
                redirectUrl = "/vendedor-dashboard";
                break;
            } else if (rol.equals("ROLE_DOMICILIARIO")) {
                redirectUrl = "/domiciliario-dashboard";
                break;
            } else if (rol.equals("ROLE_CLIENTE")) {
                redirectUrl = "/cliente-dashboard";
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}