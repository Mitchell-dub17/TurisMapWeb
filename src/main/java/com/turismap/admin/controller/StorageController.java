package com.turismap.admin.controller;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/archivos")
@CrossOrigin(origins = "*")
public class StorageController {

    @PostMapping("/subir")
    public ResponseEntity<List<String>> subirImagenes(@RequestParam("archivos") MultipartFile[] archivos) {
        List<String> urlsPublicas = new ArrayList<>();
        Bucket bucket = StorageClient.getInstance().bucket();

        for (MultipartFile archivo : archivos) {
            if (archivo.isEmpty()) continue;

            try {
                // Nombre único para evitar sobreescrituras en Storage
                String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "foto.jpg";
                String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
                String nombreDestino = "comercios/" + UUID.randomUUID() + extension;

                // Subir archivo al bucket y hacerlo público para lectura
                Blob blob = bucket.create(
                        nombreDestino,
                        archivo.getBytes(),
                        archivo.getContentType()
                );

                // Construir URL pública permanente de Google Cloud / Firebase
                String urlDescarga = String.format(
                        "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                        bucket.getName(),
                        URLEncoder.encode(nombreDestino, StandardCharsets.UTF_8)
                );

                urlsPublicas.add(urlDescarga);
            } catch (IOException e) {
                log.error("Error al subir archivo a Firebase Storage: {}", e.getMessage());
                return ResponseEntity.internalServerError().build();
            }
        }

        return ResponseEntity.ok(urlsPublicas);
    }
}