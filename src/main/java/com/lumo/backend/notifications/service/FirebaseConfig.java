package com.lumo.backend.notifications.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.firebase.project-id:notification-alphroes}")
    private String projectId;

    @Value("${app.firebase.credentials-file:}")
    private String credentialsFile;

    @Value("${app.firebase.client-email:}")
    private String clientEmail;

    @Value("${app.firebase.private-key:}")
    private String privateKey;

    private boolean initialized = false;

    @PostConstruct
    public void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) {
            this.initialized = true;
            log.info("[Firebase] FirebaseApp already initialized.");
            return;
        }

        try {
            FirebaseOptions options = null;

            // 1. Try explicitly configured file path
            if (credentialsFile != null && !credentialsFile.isBlank()) {
                File file = new File(credentialsFile);
                if (file.exists()) {
                    try (InputStream serviceAccount = new FileInputStream(file)) {
                        options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .setProjectId(projectId)
                                .build();
                        log.info("[Firebase] Initialized with credentials file: {}", credentialsFile);
                    }
                } else {
                    log.warn("[Firebase] Configured credentials file does not exist: {}", credentialsFile);
                }
            }

            // 1b. Check for local firebase-service-account.json in root
            if (options == null) {
                File defaultFile = new File("firebase-service-account.json");
                if (defaultFile.exists()) {
                    try (InputStream serviceAccount = new FileInputStream(defaultFile)) {
                        options = FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                                .setProjectId(projectId)
                                .build();
                        log.info("[Firebase] Initialized with default service account file: firebase-service-account.json");
                    }
                }
            }

            // 2. Try client-email and private-key from environment / properties using Jackson
            if (options == null && clientEmail != null && !clientEmail.isBlank() && privateKey != null && !privateKey.isBlank()) {
                String cleanKey = privateKey.replace("\\n", "\n").trim();
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode node = mapper.createObjectNode();
                node.put("type", "service_account");
                node.put("project_id", projectId);
                node.put("client_email", clientEmail.trim());
                node.put("private_key", cleanKey);
                String serviceAccountJson = mapper.writeValueAsString(node);

                try (InputStream stream = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))) {
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(stream))
                            .setProjectId(projectId)
                            .build();
                    log.info("[Firebase] Initialized with environment credentials for project: {}", projectId);
                }
            }

            // 3. Fallback: Google Application Default Credentials
            if (options == null) {
                try {
                    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
                    options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .setProjectId(projectId)
                            .build();
                    log.info("[Firebase] Initialized with Application Default Credentials.");
                } catch (Exception e) {
                    log.warn("[Firebase] No valid credentials found. Firebase Admin is in standby mode.");
                }
            }

            if (options != null) {
                FirebaseApp.initializeApp(options);
                this.initialized = true;
                log.info("[Firebase] FirebaseApp initialized successfully for project [{}]", projectId);
            }
        } catch (Exception e) {
            log.error("[Firebase] Error during Firebase initialization: {}", e.getMessage(), e);
        }
    }

    public boolean isInitialized() {
        return this.initialized && !FirebaseApp.getApps().isEmpty();
    }
}
