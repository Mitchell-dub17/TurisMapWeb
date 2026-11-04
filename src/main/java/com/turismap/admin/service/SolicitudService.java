package com.turismap.admin.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.turismap.admin.model.PlanComercio;
import com.turismap.admin.model.SolicitudRegistroComercio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudService {

    private static final String COLECCION = "solicitudes_comercio";
    private final PlanComercioService planComercioService;

    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    public CompletableFuture<String> guardarSolicitud(SolicitudRegistroComercio solicitud) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String id = UUID.randomUUID().toString();
                solicitud.setId(id);
                solicitud.setEstado("PENDIENTE");

                solicitud.setFechaRegistro(Instant.now().toString());

                getFirestore().collection(COLECCION).document(id).set(solicitud).get();
                log.info("Solicitud de registro de comercio registrada con ID: {}, Plan: {}", id, solicitud.getPlanNombre());
                return id;
            } catch (Exception e) {
                log.error("Error al guardar la solicitud en Firestore", e);
                throw new RuntimeException("Error al registrar solicitud: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<List<SolicitudRegistroComercio>> obtenerTodas() {
        return CompletableFuture.supplyAsync(() -> {
            List<SolicitudRegistroComercio> lista = new ArrayList<>();
            try {
                var querySnapshot = getFirestore().collection(COLECCION).get().get();
                for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                    SolicitudRegistroComercio s = doc.toObject(SolicitudRegistroComercio.class);
                    s.setId(doc.getId());
                    if (s.getPlanId() == null || s.getPlanId().isBlank()) {
                        s.setPlanId(obtenerPlanId(doc.getData()));
                    }
                    lista.add(s);
                }
            } catch (Exception e) {
                log.error("Error al obtener solicitudes de Firestore", e);
                throw new CompletionException(e);
            }
            return lista;
        }).thenCompose(solicitudes -> {
            Map<String, CompletableFuture<PlanComercio>> planesPorId = new LinkedHashMap<>();
            for (SolicitudRegistroComercio solicitud : solicitudes) {
                String planId = solicitud.getPlanId();
                if (planId != null && !planId.isBlank()) {
                    planesPorId.computeIfAbsent(planId, id ->
                            planComercioService.obtenerPlanPorId(id).exceptionally(error -> {
                                log.warn("No se pudo resolver el plan {} de una solicitud", id, error);
                                return null;
                            }));
                }
            }

            return CompletableFuture.allOf(planesPorId.values().toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> {
                        solicitudes.forEach(solicitud -> {
                            CompletableFuture<PlanComercio> planFuture = planesPorId.get(solicitud.getPlanId());
                            if (planFuture != null) {
                                PlanComercio plan = planFuture.join();
                                if (plan != null && plan.getNombre() != null && !plan.getNombre().isBlank()) {
                                    solicitud.setPlanNombre(plan.getNombre());
                                }
                            }
                        });
                        return solicitudes;
                    });
        });
    }

    private String obtenerPlanId(Map<String, Object> datos) {
        Object planId = datos.get("planId");
        if (planId instanceof String id && !id.isBlank()) {
            return id;
        }

        Object plan = datos.get("plan");
        if (plan instanceof Map<?, ?> planDatos) {
            Object id = planDatos.get("id");
            if (id instanceof String nestedId && !nestedId.isBlank()) {
                return nestedId;
            }
        }
        return null;
    }

    public CompletableFuture<Boolean> actualizarEstado(String id, String nuevoEstado) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                getFirestore().collection(COLECCION).document(id)
                        .update("estado", nuevoEstado.toUpperCase()).get();
                log.info("Solicitud {} actualizada a estado {}", id, nuevoEstado);
                return true;
            } catch (Exception e) {
                log.error("Error al actualizar estado de la solicitud {}", id, e);
                return false;
            }
        });
    }
}