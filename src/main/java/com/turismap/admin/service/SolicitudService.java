package com.turismap.admin.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.turismap.admin.model.SolicitudPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class SolicitudService {

    private static final String COLECCION = "solicitudes_basico";

    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    public CompletableFuture<String> guardarSolicitud(SolicitudPlan solicitud) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String id = UUID.randomUUID().toString();
                solicitud.setId(id);
                solicitud.setPlan("BASICO");
                solicitud.setEstado("PENDIENTE");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm a");
                solicitud.setFechaRegistro(LocalDateTime.now().format(formatter));

                getFirestore().collection(COLECCION).document(id).set(solicitud).get();
                log.info("Solicitud de plan básico registrada con ID: {}", id);
                return id;
            } catch (Exception e) {
                log.error("Error al guardar la solicitud en Firestore", e);
                throw new RuntimeException("Error al registrar solicitud: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<List<SolicitudPlan>> obtenerTodas() {
        return CompletableFuture.supplyAsync(() -> {
            List<SolicitudPlan> lista = new ArrayList<>();
            try {
                var querySnapshot = getFirestore().collection(COLECCION).get().get();
                for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                    SolicitudPlan s = doc.toObject(SolicitudPlan.class);
                    s.setId(doc.getId());
                    lista.add(s);
                }
            } catch (Exception e) {
                log.error("Error al obtener solicitudes de Firestore", e);
            }
            return lista;
        });
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