package com.turismap.admin.controller;

import com.turismap.admin.model.SolicitudRegistroComercio;
import com.turismap.admin.security.JwtUtil;
import com.turismap.admin.service.SolicitudService;
import com.turismap.admin.service.UsuarioFirestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final UsuarioFirestoreService usuarioFirestoreService;
    private final JwtUtil jwtUtil;

    @GetMapping("/planes")
    public CompletableFuture<ResponseEntity<?>> obtenerPlanesActivos() {
        return solicitudService.obtenerPlanesActivos()
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al cargar planes activos", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("No se pudieron cargar los planes");
                });
    }

    @GetMapping("/opciones")
    @PreAuthorize("isAuthenticated()")
    public CompletableFuture<ResponseEntity<?>> obtenerOpciones() {
        return solicitudService.obtenerOpciones()
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al cargar opciones de solicitud", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("No se pudieron cargar las opciones de la solicitud");
                });
    }

    // El servidor toma la identidad del usuario de la sesión verificada, no del formulario.
    @PostMapping("/registrar")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> registrarSolicitud(
            @RequestBody SolicitudRegistroComercio solicitud,
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String firebaseUid;
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Inicia sesión para enviar una solicitud."));
            }
            firebaseUid = jwtUtil.extractClaim(
                    authorization.substring(7).trim(),
                    claims -> claims.get("firebaseUid", String.class)
            );
        } catch (Exception ex) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("La sesión expiró. Inicia sesión nuevamente."));
        }

        if (firebaseUid == null || firebaseUid.isBlank() || authentication == null
                || authentication.getName() == null || authentication.getName().isBlank()) {
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("La sesión no contiene la identidad necesaria para enviar la solicitud."));
        }

        return usuarioFirestoreService.obtenerUsuarioPorEmail(authentication.getName())
                .thenCompose(usuario -> {
                    if (usuario == null || usuario.getNombre() == null || usuario.getNombre().isBlank()) {
                        return CompletableFuture.failedFuture(
                                new IllegalArgumentException("No se encontró el perfil asociado a esta sesión."));
                    }
                    solicitud.setPropietarioUid(firebaseUid);
                    solicitud.setEmail(authentication.getName());
                    solicitud.setNombreCompleto(usuario.getNombre());
                    solicitud.setPlanNombre(null);
                    return solicitudService.guardarSolicitud(solicitud, firebaseUid, authentication.getName());
                })
                .<ResponseEntity<?>>thenApply(id -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("mensaje", "Solicitud enviada con éxito", "id", id)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    if (cause instanceof IllegalArgumentException) {
                        return ResponseEntity.badRequest().body(cause.getMessage());
                    }
                    log.error("Error al procesar solicitud de comercio", cause);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error al procesar la solicitud");
                });
    }

    // 2. ENDPOINT ADMIN: Listar todas las solicitudes recibidas
    @GetMapping("/admin/todas")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> listarSolicitudes() {
        return solicitudService.obtenerTodas()
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al consultar solicitudes"));
    }

    // 3. ENDPOINT ADMIN: Cambiar estado (APROBADO o RECHAZADO)
    @PatchMapping("/admin/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> cambiarEstado(
            @PathVariable("id") String id,
            @RequestParam("estado") String estado) {
        return solicitudService.actualizarEstado(id, estado)
                .<ResponseEntity<?>>thenApply(ok -> ok
                        ? ResponseEntity.ok(Map.of("mensaje", "Estado actualizado correctamente"))
                        : ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontró la solicitud"))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al actualizar estado"));
    }
}