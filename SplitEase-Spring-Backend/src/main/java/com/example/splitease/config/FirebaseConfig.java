


package com.example.splitease.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;


@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() throws IOException {
        String base64Credentials = System.getenv("FIREBASE_CREDENTIALS_BASE64");

        if (base64Credentials == null || base64Credentials.trim().isEmpty()) {
            throw new IllegalStateException("FIREBASE_CREDENTIALS_BASE64 environment variable is not set!");
        }

        byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);

        try (ByteArrayInputStream serviceAccount = new ByteArrayInputStream(decodedBytes)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        }
    }
}