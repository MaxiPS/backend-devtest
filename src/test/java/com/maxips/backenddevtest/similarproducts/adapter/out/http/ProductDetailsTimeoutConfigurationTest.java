package com.maxips.backenddevtest.similarproducts.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maxips.backenddevtest.similarproducts.application.exception.ProductDetailsUnavailableException;
import com.maxips.backenddevtest.similarproducts.port.out.ProductDetailsPort;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "product-api.read-timeout=100ms")
class ProductDetailsTimeoutConfigurationTest {

    private static final MockWebServer MOCK_WEB_SERVER = startMockWebServer();

    @Autowired
    private ProductDetailsPort productDetailsPort;

    @DynamicPropertySource
    static void productApiProperties(DynamicPropertyRegistry registry) {
        registry.add("product-api.base-url", () -> MOCK_WEB_SERVER.url("/").toString());
    }

    @AfterAll
    static void stopMockWebServer() {
        MOCK_WEB_SERVER.close();
    }

    @Test
    void mapsAConfiguredReadTimeoutToProductDetailsUnavailable() {
        MOCK_WEB_SERVER.enqueue(new MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("{\"id\":\"3\",\"name\":\"Blazer\",\"price\":29.99,\"availability\":false}")
                .bodyDelay(500, TimeUnit.MILLISECONDS)
                .build());

        assertThatThrownBy(() -> productDetailsPort.findProductDetails("3"))
                .isInstanceOf(ProductDetailsUnavailableException.class);
        assertThat(MOCK_WEB_SERVER.getRequestCount()).isEqualTo(1);
    }

    private static MockWebServer startMockWebServer() {
        MockWebServer mockWebServer = new MockWebServer();
        try {
            mockWebServer.start();
            return mockWebServer;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
