package com.turismap.admin.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.file}")
    private String firebaseCredentialsPath;

    @Value("${firebase.project.id}")
    private String firebaseProjectId;

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = new ClassPathResource(firebaseCredentialsPath).getInputStream();
                
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(firebaseProjectId)
                        .build();

                FirebaseApp.initializeApp(options);
                
                log.info("Firebase inicializado exitosamente con Firestore");
            }
        } catch (IOException e) {
            log.error("Error al inicializar Firebase: {}", e.getMessage());
            throw new RuntimeException("No se pudo inicializar Firebase", e);
        }
    }

    @Bean
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }
}
