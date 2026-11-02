package com.turismap.admin.controller;

import com.turismap.admin.model.SolicitudPlan;
import com.turismap.admin.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // 1. ENDPOINT PÚBLICO: Envío del formulario desde la landing
    @PostMapping("/basico")
    public CompletableFuture<ResponseEntity<?>> registrarSolicitudBasico(@RequestBody SolicitudPlan solicitud) {
        log.info("Recibida nueva solicitud de Plan Básico para: {}", solicitud.getNombreNegocio());

        if (solicitud.getNombreCompleto() == null || solicitud.getNombreCompleto().isBlank()
                || solicitud.getTelefono() == null || solicitud.getTelefono().isBlank()
                || solicitud.getNombreNegocio() == null || solicitud.getNombreNegocio().isBlank()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body("Nombre, teléfono y nombre del negocio son obligatorios."));
        }

        return solicitudService.guardarSolicitud(solicitud)
                .<ResponseEntity<?>>thenApply(id -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("mensaje", "Solicitud enviada con éxito", "id", id)))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al procesar la solicitud"));
    }

    // 2. ENDPOINT ADMIN: Listar todas las solicitudes recibidas
    @GetMapping("/admin/todas")
    public CompletableFuture<ResponseEntity<?>> listarSolicitudes() {
        return solicitudService.obtenerTodas()
                .<ResponseEntity<?>>thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al consultar solicitudes"));
    }

    // 3. ENDPOINT ADMIN: Cambiar estado (APROBADO o RECHAZADO)
    @PatchMapping("/admin/{id}/estado")
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