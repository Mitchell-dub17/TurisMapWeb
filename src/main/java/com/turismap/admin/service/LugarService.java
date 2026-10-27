package com.turismap.admin.service;

import com.turismap.admin.model.Lugar;
import com.turismap.admin.repository.FirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class LugarService {

    private final FirestoreRepository firestoreRepository;
    private static final String COLLECTION = "lugares";

    public CompletableFuture<Lugar> crearLugar(Lugar lugar) {
        if (lugar.getId() == null || lugar.getId().isEmpty()) {
            lugar.setId(generateIdFromNombre(lugar.getNombre()));
        }
        
        return firestoreRepository.create(COLLECTION, lugar.getId(), lugar);
    }

    public CompletableFuture<Lugar> actualizarLugar(String id, Lugar lugar) {
        return firestoreRepository.update(COLLECTION, id, lugar);
    }

    public CompletableFuture<Void> eliminarLugar(String id) {
        return firestoreRepository.delete(COLLECTION, id);
    }

    public CompletableFuture<Lugar> obtenerLugarPorId(String id) {
        return firestoreRepository.findById(COLLECTION, id, Lugar.class);
    }

    public CompletableFuture<List<Lugar>> obtenerTodosLugares() {
        return firestoreRepository.findAll(COLLECTION, Lugar.class);
    }

    public CompletableFuture<List<Lugar>> obtenerLugaresPorCategoria(String categoria) {
        return firestoreRepository.findByField(COLLECTION, "categoria", categoria, Lugar.class);
    }

    private String generateIdFromNombre(String nombre) {
        String normalized = java.text.Normalizer.normalize(nombre, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }
}
