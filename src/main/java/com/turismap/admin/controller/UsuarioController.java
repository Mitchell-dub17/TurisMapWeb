package com.turismap.admin.controller;

import com.turismap.admin.service.UsuarioFirestoreService;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UsuarioController {
    private final UsuarioFirestoreService usuarioFirestoreService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> obtenerTodosUsuarios() {
        return usuarioFirestoreService.obtenerTodosUsuarios()
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error obteniendo usuarios: ", ex);
                    return ResponseEntity.status(500).build();
                });
    }

    @PutMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> cambiarRolUsuario(@PathVariable String id, @RequestBody Map<String, String> body) {
        return usuarioFirestoreService.cambiarRol(id, body.get("nuevoRol"))
                .<ResponseEntity<?>>thenApply(u -> ResponseEntity.ok(Map.of("message", "Rol actualizado exitosamente")))
                .exceptionally(ex -> {
                    log.error("Error actualizando rol: ", ex);
                    return ResponseEntity.status(500).build();
                });
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> eliminarUsuario(@PathVariable String id) {
        return usuarioFirestoreService.eliminarUsuario(id)
                .<ResponseEntity<?>>thenApply(v -> ResponseEntity.ok(Map.of("message", "Usuario eliminado")))
                .exceptionally(ex -> {
                    log.error("Error al eliminar usuario: ", ex);
                    return ResponseEntity.status(500).build();
                });
    }
}
