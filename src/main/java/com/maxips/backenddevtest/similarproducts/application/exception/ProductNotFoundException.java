package com.maxips.backenddevtest.similarproducts.application.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productId) {
        super("Product '%s' was not found".formatted(productId));
    }
}
