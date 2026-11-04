package com.turismap.admin.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // 1. Habilitar despachos asíncronos para CompletableFuture
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()

                        // 2. Preflight de CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 3. Vistas web (renderizadas por WebController y JS)
                        .requestMatchers(
                                "/",
                                "/inicio",
                                "/login",
                                "/admin",
                                "/portal-cliente"
                        ).permitAll()

                        // 4. Recursos estáticos
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()

                        // 5. Autenticación y registro de cuentas
                        .requestMatchers("/api/auth/**").permitAll()

                        // 6. Consultas públicas de lugares y reseñas para la web y app
                        .requestMatchers(HttpMethod.GET, "/api/lugares/**", "/api/resenas/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/solicitudes/planes").permitAll()

                        // 7. API Administrativa exclusiva
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ==============================================================
                        // REGLAS 1 Y 2: Solo roles ADMIN y CLIENTE acceden a la gestión
                        // Los que tienen plan Básico o solo VIAJERO son rechazados aquí
                        // ==============================================================
                        .requestMatchers("/api/archivos/**").hasAnyRole("ADMIN", "CLIENTE")
                        .requestMatchers("/api/cliente/**").hasAnyRole("ADMIN", "CLIENTE")

                        // Todo lo demás requiere autenticación válida
                        .anyRequest().authenticated()
                )

                // ==============================================================
                // REGLA 3: Manejo de accesos no autorizados y falta de plan
                // ==============================================================
                .exceptionHandling(exceptions -> exceptions
                        // Cuando el usuario no envía token o el token expiró (401)
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"status\": 401, \"error\": \"No autorizado\", \"mensaje\": \"Debes iniciar sesión para continuar\"}");
                        })
                        // Cuando es un VIAJERO o no tiene el rol necesario (403)
                        .accessDeniedHandler(customAccessDeniedHandler())
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Manejador para la Regla 3:
     * Si un usuario autenticado solo como VIAJERO o con plan BASICO
     * intenta realizar acciones de cliente, devuelve el mensaje acordado.
     */
    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"status\": 403, \"error\": \"Acceso denegado\", \"mensaje\": \"Para ingresar tienes que tener plan\"}");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"
        ));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}