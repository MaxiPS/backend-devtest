package com.maxips.backenddevtest.similarproducts.adapter.in.web;

import com.maxips.backenddevtest.similarproducts.port.in.GetSimilarProductsUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimilarProductsController {

    private final GetSimilarProductsUseCase getSimilarProductsUseCase;

    public SimilarProductsController(GetSimilarProductsUseCase getSimilarProductsUseCase) {
        this.getSimilarProductsUseCase = getSimilarProductsUseCase;
    }

    @GetMapping("/product/{productId}/similar")
    public List<SimilarProductResponse> getSimilarProducts(@PathVariable("productId") String productId) {
        return getSimilarProductsUseCase.getSimilarProducts(productId).stream()
                .map(SimilarProductResponse::from)
                .toList();
    }
}
