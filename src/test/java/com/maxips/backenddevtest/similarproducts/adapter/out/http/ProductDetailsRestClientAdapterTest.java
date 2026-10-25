package com.maxips.backenddevtest.similarproducts.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maxips.backenddevtest.similarproducts.application.exception.ProductDetailsUnavailableException;
import com.maxips.backenddevtest.similarproducts.application.exception.RelatedProductNotFoundException;
import com.maxips.backenddevtest.similarproducts.domain.ProductDetail;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ProductDetailsRestClientAdapterTest {

    private final MockWebServer mockWebServer = new MockWebServer();
    private ProductDetailsRestClientAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer.start();
        RestClient restClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        adapter = new ProductDetailsRestClientAdapter(restClient);
    }

    @AfterEach
    void tearDown() {
        mockWebServer.close();
    }

    @Test
    void mapsTheProductResponseAndRequestsTheProductPath() throws InterruptedException {
        mockWebServer.enqueue(jsonResponse(
                200,
                "{\"id\":\"3\",\"name\":\"Blazer\",\"price\":29.99,\"availability\":false}"));

        ProductDetail product = adapter.findProductDetails("3");

        assertThat(product).isEqualTo(new ProductDetail("3", "Blazer", new BigDecimal("29.99"), false));
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getTarget()).isEqualTo("/product/3");
    }

    @Test
    void mapsAnUpstreamNotFoundResponseToRelatedProductNotFound() {
        mockWebServer.enqueue(jsonResponse(404, "{\"message\":\"Product not found\"}"));

        assertThatThrownBy(() -> adapter.findProductDetails("5"))
                .isInstanceOf(RelatedProductNotFoundException.class);
    }

    @Test
    void mapsAnUpstreamServerErrorToProductDetailsUnavailable() {
        mockWebServer.enqueue(jsonResponse(500, "{\"message\":\"Internal server error\"}"));

        assertThatThrownBy(() -> adapter.findProductDetails("6"))
                .isInstanceOf(ProductDetailsUnavailableException.class);
    }

    @Test
    void mapsAConnectionFailureToProductDetailsUnavailable() {
        String baseUrl = mockWebServer.url("/").toString();
        mockWebServer.close();
        ProductDetailsRestClientAdapter unavailableAdapter = new ProductDetailsRestClientAdapter(
                RestClient.builder().baseUrl(baseUrl).build());

        assertThatThrownBy(() -> unavailableAdapter.findProductDetails("6"))
                .isInstanceOf(ProductDetailsUnavailableException.class);
    }

    @Test
    void treatsAnEmptyResponseBodyAsProductDetailsUnavailable() {
        mockWebServer.enqueue(jsonResponse(200, ""));

        assertThatThrownBy(() -> adapter.findProductDetails("3"))
                .isInstanceOf(ProductDetailsUnavailableException.class);
    }

    @Test
    void treatsAnEmptyProductIdAsProductDetailsUnavailable() {
        mockWebServer.enqueue(jsonResponse(
                200, "{\"id\":\"\",\"name\":\"Blazer\",\"price\":29.99,\"availability\":false}"));

        assertThatThrownBy(() -> adapter.findProductDetails("3"))
                .isInstanceOf(ProductDetailsUnavailableException.class);
    }

    @Test
    void treatsAnEmptyProductNameAsProductDetailsUnavailable() {
        mockWebServer.enqueue(jsonResponse(
                200, "{\"id\":\"3\",\"name\":\"\",\"price\":29.99,\"availability\":false}"));

        assertThatThrownBy(() -> adapter.findProductDetails("3"))
                .isInstanceOf(ProductDetailsUnavailableException.class);
    }

    @Test
    void treatsAWhitespaceOnlyProductNameAsProductDetailsUnavailable() {
        mockWebServer.enqueue(jsonResponse(
                200, "{\"id\":\"3\",\"name\":\"   \",\"price\":29.99,\"availability\":false}"));

        assertThatThrownBy(() -> adapter.findProductDetails("3"))
                .isInstanceOf(ProductDetailsUnavailableException.class);
    }

    @Test
    void treatsAProductIdMismatchAsProductDetailsUnavailable() {
        mockWebServer.enqueue(jsonResponse(
                200, "{\"id\":\"4\",\"name\":\"Blazer\",\"price\":29.99,\"availability\":false}"));

        assertThatThrownBy(() -> adapter.findProductDetails("3"))
                .isInstanceOf(ProductDetailsUnavailableException.class);
    }

    private MockResponse jsonResponse(int status, String body) {
        return new MockResponse.Builder()
                .code(status)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build();
    }
}
