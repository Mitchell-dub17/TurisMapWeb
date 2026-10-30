package com.turismap.admin.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.file}")
    private String firebaseCredentialsFile;

    @Value("${firebase.project.id}")
    private String firebaseProjectId;

    @Value("${firebase.storage.bucket}")
    private String firebaseStorageBucket;

    @PostConstruct
    public void initializeFirebase() {
        try {
            ClassPathResource resource = new ClassPathResource(firebaseCredentialsFile);
            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(firebaseProjectId)
                    .setStorageBucket(firebaseStorageBucket)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase inicializado exitosamente con Firestore y Storage");
            }

        } catch (IOException e) {
            log.error("Error al inicializar Firebase: {}", e.getMessage());
            throw new RuntimeException("No se pudo inicializar Firebase", e);
        }
    }

    @Bean(destroyMethod = "")
    public Firestore firestore() {
        return FirestoreClient.getFirestore();
    }
}