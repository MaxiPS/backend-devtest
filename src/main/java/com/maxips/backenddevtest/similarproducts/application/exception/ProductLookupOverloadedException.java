package com.maxips.backenddevtest.similarproducts.application.exception;

public class ProductLookupOverloadedException extends RuntimeException {

    public ProductLookupOverloadedException(String productId) {
        this(productId, null);
    }

    public ProductLookupOverloadedException(String productId, Throwable cause) {
        super("Product lookup capacity exceeded for product '%s'".formatted(productId), cause);
    }
}
