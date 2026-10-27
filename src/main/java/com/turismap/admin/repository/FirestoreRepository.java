package com.turismap.admin.repository;

import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FirestoreRepository {

    private final Firestore firestore;

    public CollectionReference getCollection(String collectionName) {
        return firestore.collection(collectionName);
    }

    public <T> CompletableFuture<T> create(String collection, String id, T data) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        getCollection(collection).document(id).set(data)
                .addListener(() -> {
                    log.info("Documento creado exitosamente en {} con ID: {}", collection, id);
                    future.complete(data);
                }, Runnable::run);
        
        return future;
    }

    public <T> CompletableFuture<T> update(String collection, String id, T data) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        getCollection(collection).document(id).set(data, SetOptions.merge())
                .addListener(() -> {
                    log.info("Documento actualizado exitosamente en {} con ID: {}", collection, id);
                    future.complete(data);
                }, Runnable::run);
        
        return future;
    }

    public CompletableFuture<Void> delete(String collection, String id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        getCollection(collection).document(id).delete()
                .addListener(() -> {
                    log.info("Documento eliminado exitosamente en {} con ID: {}", collection, id);
                    future.complete(null);
                }, Runnable::run);
        
        return future;
    }

    public <T> CompletableFuture<T> findById(String collection, String id, Class<T> clazz) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        getCollection(collection).document(id).get()
                .addListener(() -> {
                    try {
                        DocumentSnapshot snapshot = getCollection(collection).document(id).get().get();
                        if (snapshot.exists()) {
                            future.complete(snapshot.toObject(clazz));
                        } else {
                            future.complete(null);
                        }
                    } catch (Exception e) {
                        log.error("Error al buscar documento en {}: {}", collection, e.getMessage());
                        future.completeExceptionally(e);
                    }
                }, Runnable::run);
        
        return future;
    }

    public <T> CompletableFuture<List<T>> findAll(String collection, Class<T> clazz) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        
        getCollection(collection).get()
                .addListener(() -> {
                    try {
                        QuerySnapshot snapshot = getCollection(collection).get().get();
                        List<T> results = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            T item = doc.toObject(clazz);
                            if (item != null) {
                                results.add(item);
                            }
                        }
                        future.complete(results);
                    } catch (Exception e) {
                        log.error("Error al listar documentos en {}: {}", collection, e.getMessage());
                        future.completeExceptionally(e);
                    }
                }, Runnable::run);
        
        return future;
    }

    public <T> CompletableFuture<List<T>> findByField(String collection, String field, Object value, Class<T> clazz) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        
        getCollection(collection).whereEqualTo(field, value).get()
                .addListener(() -> {
                    try {
                        QuerySnapshot snapshot = getCollection(collection).whereEqualTo(field, value).get().get();
                        List<T> results = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            T item = doc.toObject(clazz);
                            if (item != null) {
                                results.add(item);
                            }
                        }
                        future.complete(results);
                    } catch (Exception e) {
                        log.error("Error al buscar por campo {} en {}: {}", field, collection, e.getMessage());
                        future.completeExceptionally(e);
                    }
                }, Runnable::run);
        
        return future;
    }
}