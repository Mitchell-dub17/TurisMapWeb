package com.turismap.admin.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.turismap.admin.model.LoginRequest;
import com.turismap.admin.model.LoginResponse;
import com.turismap.admin.model.RegistroUsuarioRequest;
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
import java.util.Map;
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

                    UserDetails userDetails = User.builder()
                            .username(usuario.getEmail())
                            .password("")
                            .authorities(Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_" + rol)))
                            .build();

                    String token = jwtUtil.generateToken(
                            userDetails,
                            Map.of("firebaseUid", decodedToken.getUid())
                    );
                    String redirectUrl = determineRedirectUrl(rol);

                    LoginResponse response = LoginResponse.builder()
                            .token(token)
                            .tipo(rol.toLowerCase(Locale.ROOT))
                            .nombre(usuario.getNombre())
                            .correo(usuario.getEmail())
                            .telefono(usuario.getTelefono())
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
            @Valid @RequestBody RegistroUsuarioRequest registro) {
        final FirebaseToken decodedToken;
        try {
            decodedToken = FirebaseAuth.getInstance().verifyIdToken(registro.getToken());
        } catch (Exception e) {
            log.warn("No se pudo validar el token de Firebase durante el registro: {}", e.getMessage());
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token de Firebase inválido o expirado"));
        }

        String email = decodedToken.getEmail();
        if (email == null || email.isBlank()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest()
                    .body("La cuenta de Firebase no tiene un correo válido."));
        }

        log.info("Intento de registro para: {}", email);
        return usuarioFirestoreService
                .obtenerUsuarioPorEmail(email)
                .<ResponseEntity<?>>thenCompose(existing -> {

                    if (existing != null) {
                        return CompletableFuture.completedFuture(
                                ResponseEntity.badRequest()
                                        .body("El correo ya está registrado"));
                    }

                    UsuarioFirestore usuario = new UsuarioFirestore();
                    usuario.setId(decodedToken.getUid());
                    usuario.setNombre(registro.getNombre().trim());
                    usuario.setEmail(email);
                    usuario.setTelefono(registro.getTelefono().trim());
                    usuario.setRol("VIAJERO");
                    usuario.setPlan("NINGUNO");

                    return usuarioFirestoreService
                            .crearUsuario(decodedToken.getUid(), usuario)
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