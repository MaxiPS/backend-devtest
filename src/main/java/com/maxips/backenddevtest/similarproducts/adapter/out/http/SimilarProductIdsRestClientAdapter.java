package com.maxips.backenddevtest.similarproducts.adapter.out.http;

import com.maxips.backenddevtest.similarproducts.application.exception.ProductNotFoundException;
import com.maxips.backenddevtest.similarproducts.application.exception.SimilarProductIdsUnavailableException;
import com.maxips.backenddevtest.similarproducts.port.out.SimilarProductIdsPort;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SimilarProductIdsRestClientAdapter implements SimilarProductIdsPort {

    private final RestClient restClient;

    public SimilarProductIdsRestClientAdapter(RestClient productApiRestClient) {
        this.restClient = productApiRestClient;
    }

    @Override
    public List<String> findSimilarProductIds(String productId) {
        try {
            List<String> similarIds = restClient.get()
                    .uri("/product/{productId}/similarids", productId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, response) -> {
                                throw new ProductNotFoundException(productId);
                            })
                    .onStatus(HttpStatusCode::isError,
                            (request, response) -> {
                                throw new SimilarProductIdsUnavailableException(
                                        productId, response.getStatusCode().value());
                            })
                    .body(new ParameterizedTypeReference<>() {});

            if (similarIds == null) {
                throw new SimilarProductIdsUnavailableException(
                        productId, new IllegalStateException("The similar IDs response body was empty"));
            }

            return List.copyOf(similarIds);
        } catch (ProductNotFoundException | SimilarProductIdsUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new SimilarProductIdsUnavailableException(productId, exception);
        }
    }
}
