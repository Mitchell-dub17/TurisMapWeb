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

    @PutMapping("/{email}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> ascenderAdmin(@PathVariable String email, @RequestBody Map<String, String> body) {
        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();
        Firestore db = FirestoreClient.getFirestore();
        
        db.collection("usuarios").whereEqualTo("email", email).get().addListener(() -> {
            try {
                for (QueryDocumentSnapshot doc : db.collection("usuarios").whereEqualTo("email", email).get().get().getDocuments()) {
                    doc.getReference().update("rol", body.get("nuevoRol"));
                }
                future.complete(ResponseEntity.ok().body(Map.of("message", "Rol actualizado exitosamente")));
            } catch (Exception e) {
                log.error("Error actualizando rol: ", e);
                future.complete(ResponseEntity.status(500).build());
            }
        }, Runnable::run);
        
        return future;
    }
}
