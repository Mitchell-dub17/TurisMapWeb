package com.turismap.admin.service;

import com.turismap.admin.model.Resena;
import com.turismap.admin.repository.FirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResenaService {

    private final FirestoreRepository firestoreRepository;
    private static final String COLLECTION = "resenas";

    public CompletableFuture<Resena> crearResena(Resena resena) {
        String resenaId = generateResenaId(resena.getTipoDestino(), resena.getDestinoId(), resena.getUsuarioUid());
        
        if (resena.getCreadoEn() == null) {
            resena.setCreadoEn(Timestamp.now());
        }
        
        if (resena.getActualizadoEn() == null) {
            resena.setActualizadoEn(Timestamp.now());
        }
        
        return firestoreRepository.create(COLLECTION, resenaId, resena);
    }

    public CompletableFuture<Resena> actualizarResena(String id, Resena resena) {
        resena.setActualizadoEn(Timestamp.now());
        return firestoreRepository.update(COLLECTION, id, resena);
    }

    public CompletableFuture<Void> eliminarResena(String id) {
        return firestoreRepository.delete(COLLECTION, id);
    }

    public CompletableFuture<Resena> obtenerResenaPorId(String id) {
        return firestoreRepository.findById(COLLECTION, id, Resena.class);
    }

    public CompletableFuture<List<Resena>> obtenerTodasResenas() {
        return firestoreRepository.findAll(COLLECTION, Resena.class);
    }

    public CompletableFuture<List<Resena>> obtenerResenasPorDestino(String tipoDestino, String destinoId) {
        return firestoreRepository.findByField(COLLECTION, "tipoDestino", tipoDestino, Resena.class)
                .thenApply(resenas -> resenas.stream()
                        .filter(r -> destinoId.equals(r.getDestinoId()))
                        .toList());
    }

    public CompletableFuture<List<Resena>> obtenerResenasPorUsuario(String usuarioUid) {
        return firestoreRepository.findByField(COLLECTION, "usuarioUid", usuarioUid, Resena.class);
    }

    private String generateResenaId(String tipoDestino, String destinoId, String usuarioUid) {
        return String.format("%s_%s_%s", tipoDestino, destinoId, usuarioUid);
    }
}
