package com.turismap.admin.service;

import com.turismap.admin.model.PlanComercio;
import com.turismap.admin.repository.FirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanComercioService {

    private final FirestoreRepository firestoreRepository;
    private static final String COLLECTION = "planes_comercio";

    public CompletableFuture<PlanComercio> crearPlan(String id, PlanComercio plan) {
        return firestoreRepository.create(COLLECTION, id, plan);
    }

    public CompletableFuture<PlanComercio> obtenerPlanPorId(String id) {
        return firestoreRepository.findById(COLLECTION, id, PlanComercio.class);
    }

    public CompletableFuture<List<PlanComercio>> obtenerTodosLosPlanes() {
        return firestoreRepository.findAll(COLLECTION, PlanComercio.class);
    }

    public CompletableFuture<PlanComercio> actualizarPlan(String id, PlanComercio plan) {
        return firestoreRepository.update(COLLECTION, id, plan);
    }

    public CompletableFuture<Void> eliminarPlan(String id) {
        return firestoreRepository.delete(COLLECTION, id);
    }
}
