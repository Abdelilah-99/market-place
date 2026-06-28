package com.buy01.users;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
		"DISCOVERY=http://localhost:8761/eureka",
		"JWT_EXP=3600000",
		"spring.kafka.bootstrap-servers=localhost:9092",
		"MONGODB_USERNAME=test",
		"MONGODB_PWD=test",
		"MONGODB_HOST=localhost",
		"MONGODB_PORT=27017",
		"MONGO_DB=test",
		"MONGODB_AUTH=admin",
		"KEY_STORE_PWD=test",
		"KEY_STORE_TYPE=PKCS12",
		"CERTIFICATE_PATH=file:/tmp/not-used.p12",
		"TRUSTSTORE_PATH=file:/tmp/not-used.p12",
		"TRUST_STORE_PWD=test",
		"server.ssl.enabled=false",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false"
})
class UsersApplicationTests {
	@DynamicPropertySource
	static void jwtKeyProperties(DynamicPropertyRegistry registry) throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		RSAPrivateKey privateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
		Path keyFile = Files.createTempFile("users-service-test-jwt-", ".pem");
		String encoded = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKey.getEncoded());
		Files.writeString(keyFile, "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n");
		registry.add("jwt.private-key", () -> keyFile.toUri().toString());
	}

	@Test
	void contextLoads() {
	}
}
