package com.buy01.users.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.util.ReflectionTestUtils;

class RsaKeyConfigTest {

    @TempDir
    private Path tempDir;

    @Test
    void rsaPrivateKeyLoadsPkcs8PemResource() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        String privateKey = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(keyPair.getPrivate().getEncoded());
        Path privateKeyFile = tempDir.resolve("jwt-private-key.pem");
        Files.writeString(privateKeyFile,
                "-----BEGIN PRIVATE KEY-----\n" + privateKey + "\n-----END PRIVATE KEY-----\n");

        RsaKeyConfig config = new RsaKeyConfig();
        ReflectionTestUtils.setField(config, "privateKeyResource", new FileSystemResource(privateKeyFile));

        RSAPrivateKey loadedPrivateKey = config.rsaPrivateKey();

        assertNotNull(loadedPrivateKey);
        assertEquals("RSA", loadedPrivateKey.getAlgorithm());
    }
}
