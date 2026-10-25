package com.maxips.backenddevtest.similarproducts.adapter.out.http;

import com.maxips.backenddevtest.similarproducts.application.exception.ProductDetailsUnavailableException;
import com.maxips.backenddevtest.similarproducts.application.exception.RelatedProductNotFoundException;
import com.maxips.backenddevtest.similarproducts.domain.ProductDetail;
import com.maxips.backenddevtest.similarproducts.port.out.ProductDetailsPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ProductDetailsRestClientAdapter implements ProductDetailsPort {

    private final RestClient restClient;

    public ProductDetailsRestClientAdapter(RestClient productApiRestClient) {
        this.restClient = productApiRestClient;
    }

    @Override
    public ProductDetail findProductDetails(String productId) {
        try {
            ProductDetailsResponse response = restClient.get()
                    .uri("/product/{productId}", productId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, clientResponse) -> {
                                throw new RelatedProductNotFoundException(productId);
                            })
                    .onStatus(HttpStatusCode::isError,
                            (request, clientResponse) -> {
                                throw new ProductDetailsUnavailableException(
                                        productId, clientResponse.getStatusCode().value());
                            })
                    .body(ProductDetailsResponse.class);

            if (response == null
                    || response.id() == null
                    || response.id().isBlank()
                    || response.name() == null
                    || response.name().isBlank()
                    || response.price() == null
                    || response.availability() == null) {
                throw new ProductDetailsUnavailableException(
                        productId, new IllegalStateException("The product details response was incomplete"));
            }
            if (!response.id().equals(productId)) {
                throw new ProductDetailsUnavailableException(
                        productId,
                        new IllegalStateException("The product details response ID did not match the request"));
            }

            return new ProductDetail(response.id(), response.name(), response.price(), response.availability());
        } catch (RelatedProductNotFoundException | ProductDetailsUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ProductDetailsUnavailableException(productId, exception);
        }
    }
}
