package com.maxips.backenddevtest.similarproducts.application.exception;

public class SimilarProductIdsUnavailableException extends RuntimeException {

    public SimilarProductIdsUnavailableException(String productId, int statusCode) {
        super("Similar products service returned HTTP %d for product '%s'".formatted(statusCode, productId));
    }

    public SimilarProductIdsUnavailableException(String productId, Throwable cause) {
        super("Could not retrieve similar product IDs for product '%s'".formatted(productId), cause);
    }
}
