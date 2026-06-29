package com.example.eureka;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

class EurekaServerApplicationTest {

    @Test
    void eurekaApplicationHasExpectedBootAnnotations() {
        assertNotNull(EurekaServerApplication.class.getAnnotation(SpringBootApplication.class));
        assertNotNull(EurekaServerApplication.class.getAnnotation(EnableEurekaServer.class));
    }

    @Test
    void mainMethodExists() throws NoSuchMethodException {
        assertNotNull(EurekaServerApplication.class.getMethod("main", String[].class));
    }
}
