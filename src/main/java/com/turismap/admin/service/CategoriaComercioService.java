package com.turismap.admin.service;

import com.turismap.admin.model.CategoriaComercio;
import com.turismap.admin.repository.FirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaComercioService {

    private final FirestoreRepository firestoreRepository;
    private static final String COLLECTION = "categorias_comercio";

    public CompletableFuture<CategoriaComercio> crearCategoria(String id, CategoriaComercio categoria) {
        return firestoreRepository.create(COLLECTION, id, categoria);
    }

    public CompletableFuture<CategoriaComercio> obtenerCategoriaPorId(String id) {
        return firestoreRepository.findById(COLLECTION, id, CategoriaComercio.class);
    }

    public CompletableFuture<List<CategoriaComercio>> obtenerTodasLasCategorias() {
        return firestoreRepository.findAll(COLLECTION, CategoriaComercio.class);
    }

    public CompletableFuture<CategoriaComercio> actualizarCategoria(String id, CategoriaComercio categoria) {
        return firestoreRepository.update(COLLECTION, id, categoria);
    }

    public CompletableFuture<Void> eliminarCategoria(String id) {
        return firestoreRepository.delete(COLLECTION, id);
    }
}
