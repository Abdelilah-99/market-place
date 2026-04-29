package com.buy01.users;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"DISCOVERY=http://localhost:8761/eureka",
		"JWT_EXP=3600000",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false"
})
class UsersApplicationTests {

	@Test
	void contextLoads() {
	}
}
