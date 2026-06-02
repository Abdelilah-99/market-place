package com.buy01.users;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"DISCOVERY=http://localhost:8761/eureka",
		"JWT_EXP=3600000",
		"jwt.private-key=classpath:keys/private.pem",
		"spring.kafka.bootstrap-servers=localhost:9092",
		"MONGODB_USERNAME=test",
		"MONGODB_PWD=test",
		"MONGODB_HOST=localhost",
		"MONGODB_PORT=27017",
		"MONGO_DB=test",
		"MONGODB_AUTH=admin",
		"KEY_STORE_PWD=test",
		"KEY_STORE_TYPE=PKCS12",
		"CERTIFICATE_PATH=classpath:keys/private.pem",
		"TRUSTSTORE_PATH=classpath:keys/private.pem",
		"TRUST_STORE_PWD=test",
		"server.ssl.enabled=false",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false"
})
class UsersApplicationTests {

	@Test
	void contextLoads() {
	}
}
