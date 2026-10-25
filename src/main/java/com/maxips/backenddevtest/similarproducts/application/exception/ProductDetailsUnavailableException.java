package com.maxips.backenddevtest.similarproducts.application.exception;

public class ProductDetailsUnavailableException extends RuntimeException {

    public ProductDetailsUnavailableException(String productId, int statusCode) {
        super("Product details service returned HTTP %d for product '%s'".formatted(statusCode, productId));
    }

    public ProductDetailsUnavailableException(String productId, Throwable cause) {
        super("Could not retrieve details for product '%s'".formatted(productId), cause);
    }
}
