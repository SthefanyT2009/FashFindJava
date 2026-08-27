package com.fashfind.fashfind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.fashfind.fashfind.service.CustomUserDetailsService;

/**
 * Configuracion principal de Spring Security para FashFind.
 * Gestiona el control de acceso basado en roles (Administrador, Vendedor,
 * Domiciliario, Cliente), el encriptado de contrasenas y la redireccion
 * post-autenticacion a traves de CustomSuccessHandler.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomSuccessHandler customSuccessHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                           CustomSuccessHandler customSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.customSuccessHandler = customSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Recursos publicos: estilos, imagenes, login, registro y pagina de inicio
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**",
                                  "/login", "/registro", "/error", "/", "/inicio").permitAll()

                // Perfil propio: cualquier usuario autenticado
                .requestMatchers("/perfil/**").authenticated()

                // Panel del cliente
                .requestMatchers("/cliente-dashboard/**").hasAnyRole("CLIENTE", "ADMINISTRADOR")

                // Carrito y pedidos: Cliente
                .requestMatchers("/carrito/**", "/pedidos/**").hasAnyRole("CLIENTE", "ADMINISTRADOR")

                // Gestion de ventas e inventario
                .requestMatchers("/vendedor-dashboard/**", "/inventario/**", "/productos/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")

                // Entregas asignadas al domiciliario
                .requestMatchers("/domiciliario-dashboard/**").hasAnyRole("DOMICILIARIO", "ADMINISTRADOR")

                // Gestion de usuarios y configuracion del sistema
                .requestMatchers("/admin/**", "/usuarios/**").hasRole("ADMINISTRADOR")

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(customSuccessHandler) // Redireccion segun el rol (cargo)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}