package com.buy01.payments.config;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import io.micrometer.observation.ObservationRegistry;

@Configuration
public class MutualTlsConfig {
    @Bean
    RestClient.Builder mutualTlsRestClientBuilder(
            @Value("${server.ssl.key-store}") Resource keyStoreResource,
            @Value("${server.ssl.key-store-password}") char[] keyStorePassword,
            @Value("${server.ssl.trust-store}") Resource trustStoreResource,
            @Value("${server.ssl.trust-store-password}") char[] trustStorePassword,
            @Value("${server.ssl.key-store-type:PKCS12}") String storeType,
            ObservationRegistry observationRegistry) throws Exception {
        KeyStore keyStore = loadStore(keyStoreResource, keyStorePassword, storeType);
        KeyStore trustStore = loadStore(trustStoreResource, trustStorePassword, storeType);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePassword);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(client))
                .observationRegistry(observationRegistry);
    }

    private KeyStore loadStore(Resource resource, char[] password, String type) throws Exception {
        KeyStore store = KeyStore.getInstance(type);
        try (InputStream input = resource.getInputStream()) {
            store.load(input, password);
        }
        return store;
    }
}
