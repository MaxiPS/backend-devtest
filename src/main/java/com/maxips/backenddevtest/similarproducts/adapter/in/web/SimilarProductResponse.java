package com.maxips.backenddevtest.similarproducts.adapter.in.web;

import com.maxips.backenddevtest.similarproducts.domain.ProductDetail;
import java.math.BigDecimal;

public record SimilarProductResponse(String id, String name, BigDecimal price, boolean availability) {

    public static SimilarProductResponse from(ProductDetail product) {
        return new SimilarProductResponse(product.id(), product.name(), product.price(), product.availability());
    }
}
