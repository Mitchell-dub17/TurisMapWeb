package com.turismap.admin.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.turismap.admin.model.LoginRequest;
import com.turismap.admin.model.LoginResponse;
import com.turismap.admin.model.UsuarioFirestore;
import com.turismap.admin.security.JwtUtil;
import com.turismap.admin.service.UsuarioFirestoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioFirestoreService usuarioFirestoreService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<?>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        log.info("Procesando login con token de Firebase");

        if (loginRequest.getToken() == null || loginRequest.getToken().isBlank()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("El token de Firebase es obligatorio"));
        }

        final FirebaseToken decodedToken;

        try {
            decodedToken = FirebaseAuth.getInstance()
                    .verifyIdToken(loginRequest.getToken());
        } catch (Exception e) {
            log.warn("No se pudo validar el token de Firebase: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("Token de Firebase inválido o expirado"));
        }

        String email = decodedToken.getEmail();

        if (email == null || email.isBlank()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("El token de Firebase no contiene un correo válido"));
        }

        return usuarioFirestoreService.obtenerUsuarioPorEmail(email)
                .<ResponseEntity<?>>thenApply(usuario -> {

                    if (usuario == null) {
                        log.warn("Usuario autenticado en Firebase pero no registrado en TurisMap: {}", email);
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("El usuario no está registrado en TurisMap");
                    }

                    String plan = (usuario.getPlan() != null)
                            ? usuario.getPlan().trim().toUpperCase(Locale.ROOT)
                            : "NINGUNO";

                    String rol = (usuario.getRol() != null && !usuario.getRol().isBlank())
                            ? usuario.getRol().trim().toUpperCase(Locale.ROOT)
                            : "VIAJERO";

                    // ==============================================================
                    // REGLA 2: Si el viajero adquirió plan Socio o Premium,
                    // se le habilita dinámicamente el acceso como CLIENTE.
                    // ==============================================================
                    if ((plan.contains("SOCIO") || plan.contains("PREMIUM")) && !rol.equals("ADMIN")) {
                        rol = "CLIENTE";
                        log.info("Usuario {} promovido a CLIENTE por contar con plan {}", email, plan);
                    }

                    // ==============================================================
                    // REGLAS 1 Y 3:
                    // Si sigue siendo VIAJERO (sin plan o con plan BASICO),
                    // no tiene permiso para entrar al portal web.
                    // ==============================================================
                    if (!rol.equals("ADMIN") && !rol.equals("CLIENTE")) {
                        log.warn("Acceso denegado a la web para {} (rol: {}, plan: {})", email, rol, plan);
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("Para ingresar tienes que tener plan");
                    }

                    UserDetails userDetails = User.builder()
                            .username(usuario.getEmail())
                            .password("")
                            .authorities(Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_" + rol)))
                            .build();

                    String token = jwtUtil.generateToken(userDetails);
                    String redirectUrl = determineRedirectUrl(rol);

                    LoginResponse response = LoginResponse.builder()
                            .token(token)
                            .tipo(rol.toLowerCase(Locale.ROOT))
                            .nombre(usuario.getNombre())
                            .correo(usuario.getEmail())
                            .redirectUrl(redirectUrl)
                            .build();

                    log.info("Inicio de sesión completado para {} con rol {}", email, rol);
                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    log.error("Error consultando el usuario en Firestore", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error interno al consultar el usuario");
                });
    }

    @PostMapping("/registro")
    public CompletableFuture<ResponseEntity<?>> registro(
            @Valid @RequestBody UsuarioFirestore usuario) {

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest()
                            .body("El correo electrónico es obligatorio"));
        }

        log.info("Intento de registro para: {}", usuario.getEmail());

        return usuarioFirestoreService
                .obtenerUsuarioPorEmail(usuario.getEmail())
                .<ResponseEntity<?>>thenCompose(existing -> {

                    if (existing != null) {
                        return CompletableFuture.completedFuture(
                                ResponseEntity.badRequest()
                                        .body("El correo ya está registrado"));
                    }

                    // ==============================================================
                    // ASIGNACIÓN DE ROLES Y PLANES SEGÚN SELECCIÓN EN LA WEB
                    // ==============================================================
                    String planInput = (usuario.getPlan() != null)
                            ? usuario.getPlan().trim().toUpperCase(Locale.ROOT)
                            : "";

                    if (planInput.contains("PREMIUM")) {
                        // Plan Socio Premium ($25)
                        usuario.setRol("CLIENTE");
                        usuario.setPlan("PREMIUM");
                    } else if (planInput.contains("SOCIO")) {
                        // Plan Socio TurisMap ($15)
                        usuario.setRol("CLIENTE");
                        usuario.setPlan("SOCIO");
                    } else if (planInput.contains("BASICO") || planInput.contains("BÁSICO")) {
                        // REGLA 1: Presencia Básica ($5) crea cuenta pero queda como VIAJERO sin portal
                        usuario.setRol("VIAJERO");
                        usuario.setPlan("BASICO");
                    } else {
                        // Registro común de turista o sin selección previa
                        if (usuario.getRol() == null || usuario.getRol().isBlank()) {
                            usuario.setRol("VIAJERO");
                        }
                        usuario.setPlan("NINGUNO");
                    }

                    String uid = java.util.UUID.randomUUID().toString();

                    return usuarioFirestoreService
                            .crearUsuario(uid, usuario)
                            .<ResponseEntity<?>>thenApply(created ->
                                    ResponseEntity.status(HttpStatus.CREATED)
                                            .body("Usuario registrado exitosamente"));
                })
                .exceptionally(ex -> {
                    log.error("Error durante el registro en Firestore", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error interno durante el registro");
                });
    }

    private String determineRedirectUrl(String rol) {
        if (rol == null) {
            return "/";
        }

        return switch (rol.trim().toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> "/admin";
            case "CLIENTE" -> "/portal-cliente";
            default -> "/";
        };
    }
}