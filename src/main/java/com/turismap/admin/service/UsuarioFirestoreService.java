package com.turismap.admin.service;

import com.turismap.admin.model.UsuarioFirestore;
import com.turismap.admin.repository.FirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioFirestoreService {

    private final FirestoreRepository firestoreRepository;
    private static final String COLLECTION = "usuarios";

    public CompletableFuture<UsuarioFirestore> crearUsuario(String uid, UsuarioFirestore usuario) {
        return firestoreRepository.create(COLLECTION, uid, usuario);
    }

    public CompletableFuture<UsuarioFirestore> actualizarUsuario(String uid, UsuarioFirestore usuario) {
        return firestoreRepository.update(COLLECTION, uid, usuario);
    }

    public CompletableFuture<Void> eliminarUsuario(String id) {
        return firestoreRepository.delete(COLLECTION, id);
    }

    public CompletableFuture<UsuarioFirestore> obtenerUsuarioPorId(String id) {
        return firestoreRepository.findById(COLLECTION, id, UsuarioFirestore.class);
    }

    public CompletableFuture<UsuarioFirestore> obtenerUsuarioPorEmail(String email) {
        return firestoreRepository.findByField(COLLECTION, "email", email, UsuarioFirestore.class)
                .thenApply(usuarios -> usuarios.stream().findFirst().orElse(null));
    }

    public CompletableFuture<List<UsuarioFirestore>> obtenerTodosUsuarios() {
        return firestoreRepository.findAll(COLLECTION, UsuarioFirestore.class);
    }

    public CompletableFuture<UsuarioFirestore> cambiarRol(String id, String nuevoRol) {
        return firestoreRepository.findById(COLLECTION, id, UsuarioFirestore.class)
                .thenCompose(usuario -> {
                    if (usuario == null) {
                        return CompletableFuture.failedFuture(new RuntimeException("Usuario no encontrado"));
                    }
                    usuario.setRol(nuevoRol);
                    return firestoreRepository.update(COLLECTION, id, usuario);
                });
    }

    public CompletableFuture<List<UsuarioFirestore>> obtenerUsuariosPorRol(String rol) {
        return firestoreRepository.findByField(COLLECTION, "rol", rol, UsuarioFirestore.class);
    }
}
