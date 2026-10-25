package com.maxips.backenddevtest.similarproducts.application.exception;

public class RelatedProductNotFoundException extends RuntimeException {

    public RelatedProductNotFoundException(String productId) {
        super("Related product '%s' was not found".formatted(productId));
    }
}
