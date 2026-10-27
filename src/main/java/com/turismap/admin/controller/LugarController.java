package com.turismap.admin.controller;

import com.turismap.admin.model.Lugar;
import com.turismap.admin.service.LugarService;
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
@RequestMapping("/api/lugares")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LugarController {

    private final LugarService lugarService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> crearLugar(@Valid @RequestBody Lugar lugar) {
        log.info("Creando lugar: {}", lugar.getNombre());
        
        return lugarService.crearLugar(lugar)
                .<ResponseEntity<?>>thenApply(created -> ResponseEntity.ok(created))
                .exceptionally(ex -> {
                    log.error("Error al crear lugar: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al crear lugar");
                });
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> actualizarLugar(
            @PathVariable String id,
            @Valid @RequestBody Lugar lugar) {
        log.info("Actualizando lugar: {}", id);
        
        return lugarService.actualizarLugar(id, lugar)
                .<ResponseEntity<?>>thenApply(updated -> ResponseEntity.ok(updated))
                .exceptionally(ex -> {
                    log.error("Error al actualizar lugar: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al actualizar lugar");
                });
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> eliminarLugar(@PathVariable String id) {
        log.info("Eliminando lugar: {}", id);
        
        return lugarService.eliminarLugar(id)
                .<ResponseEntity<?>>thenApply(v -> ResponseEntity.ok("Lugar eliminado"))
                .exceptionally(ex -> {
                    log.error("Error al eliminar lugar: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al eliminar lugar");
                });
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerLugar(@PathVariable String id) {
        return lugarService.obtenerLugarPorId(id)
                .<ResponseEntity<?>>thenApply(lugar -> {
                    if (lugar == null) {
                        return ResponseEntity.notFound().build();
                    }
                    return ResponseEntity.ok(lugar);
                })
                .exceptionally(ex -> {
                    log.error("Error al obtener lugar: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener lugar");
                });
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerTodosLugares() {
        return lugarService.obtenerTodosLugares()
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al obtener lugares: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener lugares");
                });
    }

    @GetMapping("/categoria/{categoria}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerLugaresPorCategoria(@PathVariable String categoria) {
        return lugarService.obtenerLugaresPorCategoria(categoria)
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al obtener lugares por categoría: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener lugares");
                });
    }
}
