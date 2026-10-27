package com.turismap.admin.controller;

import com.turismap.admin.model.Resena;
import com.turismap.admin.service.ResenaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/resenas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ResenaController {

    private final ResenaService resenaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> crearResena(@Valid @RequestBody Resena resena) {
        log.info("Creando reseña para destino: {}", resena.getDestinoId());
        
        return resenaService.crearResena(resena)
                .<ResponseEntity<?>>thenApply(created -> ResponseEntity.ok(created))
                .exceptionally(ex -> {
                    log.error("Error al crear reseña: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al crear reseña");
                });
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> actualizarResena(
            @PathVariable String id,
            @Valid @RequestBody Resena resena) {
        log.info("Actualizando reseña: {}", id);
        
        return resenaService.actualizarResena(id, resena)
                .<ResponseEntity<?>>thenApply(updated -> ResponseEntity.ok(updated))
                .exceptionally(ex -> {
                    log.error("Error al actualizar reseña: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al actualizar reseña");
                });
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> eliminarResena(@PathVariable String id) {
        log.info("Eliminando reseña: {}", id);
        
        return resenaService.eliminarResena(id)
                .<ResponseEntity<?>>thenApply(v -> ResponseEntity.ok("Reseña eliminada"))
                .exceptionally(ex -> {
                    log.error("Error al eliminar reseña: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al eliminar reseña");
                });
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerResena(@PathVariable String id) {
        return resenaService.obtenerResenaPorId(id)
                .<ResponseEntity<?>>thenApply(resena -> {
                    if (resena == null) {
                        return ResponseEntity.notFound().build();
                    }
                    return ResponseEntity.ok(resena);
                })
                .exceptionally(ex -> {
                    log.error("Error al obtener reseña: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener reseña");
                });
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> obtenerTodasResenas() {
        return resenaService.obtenerTodasResenas()
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al obtener reseñas: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener reseñas");
                });
    }

    @GetMapping("/destino/{tipoDestino}/{destinoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerResenasPorDestino(
            @PathVariable String tipoDestino,
            @PathVariable String destinoId) {
        return resenaService.obtenerResenasPorDestino(tipoDestino, destinoId)
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al obtener reseñas por destino: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener reseñas");
                });
    }

    @GetMapping("/usuario/{usuarioUid}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerResenasPorUsuario(@PathVariable String usuarioUid) {
        return resenaService.obtenerResenasPorUsuario(usuarioUid)
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al obtener reseñas por usuario: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener reseñas");
                });
    }
}
