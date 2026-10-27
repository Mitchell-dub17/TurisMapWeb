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

        if (loginRequest.getToken() == null
                || loginRequest.getToken().isBlank()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body("El token de Firebase es obligatorio"));
        }

        final FirebaseToken decodedToken;

        try {
            decodedToken = FirebaseAuth.getInstance()
                    .verifyIdToken(loginRequest.getToken());

        } catch (Exception e) {
            log.warn("No se pudo validar el token de Firebase: {}",
                    e.getMessage());

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
                        log.warn("Usuario autenticado en Firebase pero no registrado en TurisMap: {}",
                                email);

                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("El usuario no está registrado en TurisMap");
                    }

                    if (usuario.getRol() == null
                            || usuario.getRol().isBlank()) {
                        log.error("El usuario no tiene un rol configurado: {}",
                                email);

                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("El usuario no tiene un rol asignado");
                    }

                    String rol = usuario.getRol()
                            .trim()
                            .toUpperCase(Locale.ROOT);

                    if (!rol.equals("ADMIN") && !rol.equals("CLIENTE")) {
                        log.warn("Rol no permitido para el usuario {}: {}",
                                email, rol);

                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("El rol del usuario no está autorizado");
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

                    log.info("Inicio de sesión completado para {} con rol {}",
                            email, rol);

                    return ResponseEntity.ok(response);
                })
                .exceptionally(ex -> {
                    log.error("Error consultando el usuario en Firestore",
                            ex);

                    return ResponseEntity.status(
                                    HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error interno al consultar el usuario");
                });
    }

    @PostMapping("/registro")
    public CompletableFuture<ResponseEntity<?>> registro(
            @Valid @RequestBody UsuarioFirestore usuario) {

        if (usuario.getEmail() == null
                || usuario.getEmail().isBlank()) {
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

                    String uid = java.util.UUID.randomUUID().toString();

                    return usuarioFirestoreService
                            .crearUsuario(uid, usuario)
                            .<ResponseEntity<?>>thenApply(created ->
                                    ResponseEntity.status(HttpStatus.CREATED)
                                            .body("Usuario registrado exitosamente"));
                })
                .exceptionally(ex -> {
                    log.error("Error durante el registro en Firestore", ex);

                    return ResponseEntity.status(
                                    HttpStatus.INTERNAL_SERVER_ERROR)
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
