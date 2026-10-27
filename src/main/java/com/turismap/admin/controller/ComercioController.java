package com.turismap.admin.controller;

import com.turismap.admin.model.Comercio;
import com.turismap.admin.service.ComercioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/comercios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComercioController {

    private final ComercioService comercioService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> crearComercio(
            @Valid @RequestBody Comercio comercio,
            Authentication authentication) {
        log.info("Creando comercio: {}", comercio.getNombre());
        
        return comercioService.crearComercio(comercio)
                .<ResponseEntity<?>>thenApply(created -> ResponseEntity.ok(created))
                .exceptionally(ex -> {
                    log.error("Error al crear comercio: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al crear comercio");
                });
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> actualizarComercio(
            @PathVariable String id,
            @Valid @RequestBody Comercio comercio) {
        log.info("Actualizando comercio: {}", id);
        
        return comercioService.actualizarComercio(id, comercio)
                .<ResponseEntity<?>>thenApply(updated -> ResponseEntity.ok(updated))
                .exceptionally(ex -> {
                    log.error("Error al actualizar comercio: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al actualizar comercio");
                });
    }

    @PutMapping("/{id}/publicar")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> publicarComercio(@PathVariable String id) {
        log.info("Publicando comercio: {}", id);
        
        return comercioService.publicarComercio(id)
                .<ResponseEntity<?>>thenApply(updated -> ResponseEntity.ok(updated))
                .exceptionally(ex -> {
                    log.error("Error al publicar comercio: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al publicar comercio");
                });
    }

    @PutMapping("/{id}/destacar")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> destacarComercio(
            @PathVariable String id,
            @RequestParam boolean destacado) {
        log.info("Destacando comercio: {}, destacado: {}", id, destacado);
        
        return comercioService.destacarComercio(id, destacado)
                .<ResponseEntity<?>>thenApply(updated -> ResponseEntity.ok(updated))
                .exceptionally(ex -> {
                    log.error("Error al destacar comercio: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al destacar comercio");
                });
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> eliminarComercio(@PathVariable String id) {
        log.info("Eliminando comercio: {}", id);
        
        return comercioService.eliminarComercio(id)
                .<ResponseEntity<?>>thenApply(v -> ResponseEntity.ok("Comercio eliminado"))
                .exceptionally(ex -> {
                    log.error("Error al eliminar comercio: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al eliminar comercio");
                });
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerComercio(@PathVariable String id) {
        return comercioService.obtenerComercioPorId(id)
                .<ResponseEntity<?>>thenApply(comercio -> {
                    if (comercio == null) {
                        return ResponseEntity.notFound().build();
                    }
                    return ResponseEntity.ok(comercio);
                })
                .exceptionally(ex -> {
                    log.error("Error al obtener comercio: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener comercio");
                });
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerTodosComercios() {
        return comercioService.obtenerTodosComercios()
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al obtener comercios: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener comercios");
                });
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<?>> obtenerComerciosPorEstado(@PathVariable String estado) {
        return comercioService.obtenerComerciosPorEstado(estado)
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al obtener comercios por estado: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener comercios");
                });
    }

    @GetMapping("/propietario/{propietarioUid}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerComerciosPorPropietario(@PathVariable String propietarioUid) {
        return comercioService.obtenerComerciosPorPropietario(propietarioUid)
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al obtener comercios por propietario: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener comercios");
                });
    }

    @GetMapping("/lugar/{lugarId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerComerciosPorLugar(@PathVariable String lugarId) {
        return comercioService.obtenerComerciosPorLugar(lugarId)
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al obtener comercios por lugar: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener comercios");
                });
    }

    @GetMapping("/publicados")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerComerciosPublicados() {
        return comercioService.obtenerComerciosPublicados()
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al obtener comercios publicados: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener comercios");
                });
    }

    @GetMapping("/destacados")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public CompletableFuture<ResponseEntity<?>> obtenerComerciosDestacados() {
        return comercioService.obtenerComerciosDestacados()
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error al obtener comercios destacados: {}", ex.getMessage());
                    return ResponseEntity.status(500).body("Error al obtener comercios");
                });
    }
}
