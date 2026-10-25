package com.maxips.backenddevtest.similarproducts.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maxips.backenddevtest.similarproducts.application.exception.ProductNotFoundException;
import com.maxips.backenddevtest.similarproducts.application.exception.SimilarProductIdsUnavailableException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SimilarProductIdsRestClientAdapterTest {

    private final MockWebServer mockWebServer = new MockWebServer();
    private SimilarProductIdsRestClientAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer.start();
        RestClient restClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        adapter = new SimilarProductIdsRestClientAdapter(restClient);
    }

    @AfterEach
    void tearDown() {
        mockWebServer.close();
    }

    @Test
    void returnsSimilarIdsAsStringsInTheUpstreamOrder() throws InterruptedException {
        mockWebServer.enqueue(jsonResponse(200, "[2,3,4]"));

        List<String> similarIds = adapter.findSimilarProductIds("1");

        assertThat(similarIds).containsExactly("2", "3", "4");
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getTarget()).isEqualTo("/product/1/similarids");
    }

    @Test
    void returnsAnEmptyListWhenThereAreNoSimilarIds() {
        mockWebServer.enqueue(jsonResponse(200, "[]"));

        assertThat(adapter.findSimilarProductIds("1")).isEmpty();
    }

    @Test
    void treatsAnEmptyResponseBodyAsAnUnavailableUpstream() {
        mockWebServer.enqueue(jsonResponse(200, ""));

        assertThatThrownBy(() -> adapter.findSimilarProductIds("1"))
                .isInstanceOf(SimilarProductIdsUnavailableException.class);
    }

    @Test
    void treatsANullSimilarIdAsAnUnavailableUpstream() {
        mockWebServer.enqueue(jsonResponse(200, "[null]"));

        assertThatThrownBy(() -> adapter.findSimilarProductIds("1"))
                .isInstanceOf(SimilarProductIdsUnavailableException.class);
    }

    @Test
    void treatsABlankSimilarIdAsAnUnavailableUpstream() {
        mockWebServer.enqueue(jsonResponse(200, "[\" \" ]"));

        assertThatThrownBy(() -> adapter.findSimilarProductIds("1"))
                .isInstanceOf(SimilarProductIdsUnavailableException.class);
    }

    @Test
    void mapsAnUpstreamNotFoundResponseToProductNotFound() {
        mockWebServer.enqueue(jsonResponse(404, "{\"message\":\"Product not found\"}"));

        assertThatThrownBy(() -> adapter.findSimilarProductIds("missing"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void mapsAnUpstreamServerErrorToSimilarIdsUnavailable() {
        mockWebServer.enqueue(jsonResponse(503, "{\"message\":\"Service unavailable\"}"));

        assertThatThrownBy(() -> adapter.findSimilarProductIds("1"))
                .isInstanceOf(SimilarProductIdsUnavailableException.class);
    }

    @Test
    void mapsAConnectionFailureToSimilarIdsUnavailable() {
        String baseUrl = mockWebServer.url("/").toString();
        mockWebServer.close();
        SimilarProductIdsRestClientAdapter unavailableAdapter = new SimilarProductIdsRestClientAdapter(
                RestClient.builder().baseUrl(baseUrl).build());

        assertThatThrownBy(() -> unavailableAdapter.findSimilarProductIds("1"))
                .isInstanceOf(SimilarProductIdsUnavailableException.class);
    }

    private MockResponse jsonResponse(int status, String body) {
        return new MockResponse.Builder()
                .code(status)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build();
    }
}
