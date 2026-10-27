package com.turismap.admin.service;

import com.turismap.admin.model.Comercio;
import com.turismap.admin.repository.FirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComercioService {

    private final FirestoreRepository firestoreRepository;
    private static final String COLLECTION = "comercios";

    public CompletableFuture<Comercio> crearComercio(Comercio comercio) {
        if (comercio.getId() == null || comercio.getId().isEmpty()) {
            comercio.setId(UUID.randomUUID().toString());
        }
        
        if (comercio.getEstado() == null || comercio.getEstado().isEmpty()) {
            comercio.setEstado("pendiente");
        }
        
        if (comercio.getCreadoEn() == null) {
            comercio.setCreadoEn(Timestamp.now());
        }
        
        if (comercio.getActualizadoEn() == null) {
            comercio.setActualizadoEn(Timestamp.now());
        }
        
        return firestoreRepository.create(COLLECTION, comercio.getId(), comercio);
    }

    public CompletableFuture<Comercio> actualizarComercio(String id, Comercio comercio) {
        comercio.setId(id);
        comercio.setActualizadoEn(Timestamp.now());
        return firestoreRepository.update(COLLECTION, id, comercio);
    }

    public CompletableFuture<Comercio> publicarComercio(String id) {
        return firestoreRepository.findById(COLLECTION, id, Comercio.class)
                .thenCompose(comercio -> {
                    if (comercio == null) {
                        return CompletableFuture.failedFuture(new RuntimeException("Comercio no encontrado"));
                    }
                    comercio.setEstado("publicado");
                    comercio.setActualizadoEn(Timestamp.now());
                    return firestoreRepository.update(COLLECTION, id, comercio);
                });
    }

    public CompletableFuture<Comercio> destacarComercio(String id, boolean destacado) {
        return firestoreRepository.findById(COLLECTION, id, Comercio.class)
                .thenCompose(comercio -> {
                    if (comercio == null) {
                        return CompletableFuture.failedFuture(new RuntimeException("Comercio no encontrado"));
                    }
                    comercio.setDestacado(destacado);
                    comercio.setActualizadoEn(Timestamp.now());
                    return firestoreRepository.update(COLLECTION, id, comercio);
                });
    }

    public CompletableFuture<Void> eliminarComercio(String id) {
        return firestoreRepository.delete(COLLECTION, id);
    }

    public CompletableFuture<Comercio> obtenerComercioPorId(String id) {
        return firestoreRepository.findById(COLLECTION, id, Comercio.class);
    }

    public CompletableFuture<List<Comercio>> obtenerTodosComercios() {
        return firestoreRepository.findAll(COLLECTION, Comercio.class);
    }

    public CompletableFuture<List<Comercio>> obtenerComerciosPorEstado(String estado) {
        return firestoreRepository.findByField(COLLECTION, "estado", estado, Comercio.class);
    }

    public CompletableFuture<List<Comercio>> obtenerComerciosPorPropietario(String propietarioUid) {
        return firestoreRepository.findByField(COLLECTION, "propietarioUid", propietarioUid, Comercio.class);
    }

    public CompletableFuture<List<Comercio>> obtenerComerciosPorLugar(String lugarId) {
        return firestoreRepository.findByField(COLLECTION, "lugarId", lugarId, Comercio.class);
    }

    public CompletableFuture<List<Comercio>> obtenerComerciosPublicados() {
        return firestoreRepository.findByField(COLLECTION, "estado", "publicado", Comercio.class);
    }

    public CompletableFuture<List<Comercio>> obtenerComerciosDestacados() {
        return firestoreRepository.findByField(COLLECTION, "destacado", true, Comercio.class);
    }
}
