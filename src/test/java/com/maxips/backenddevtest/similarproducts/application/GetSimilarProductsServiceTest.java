package com.maxips.backenddevtest.similarproducts.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.maxips.backenddevtest.similarproducts.domain.ProductDetail;
import com.maxips.backenddevtest.similarproducts.port.out.ProductDetailsPort;
import com.maxips.backenddevtest.similarproducts.port.out.SimilarProductIdsPort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GetSimilarProductsServiceTest {

    @Test
    void returnsProductDetailsInSimilarIdOrder() {
        List<String> calls = new ArrayList<>();
        Map<String, ProductDetail> products = Map.of(
                "2", product("2", "Dress"),
                "3", product("3", "Blazer"));
        SimilarProductIdsPort similarProductIdsPort = productId -> {
            calls.add("similarIds:" + productId);
            return List.of("3", "2");
        };
        ProductDetailsPort productDetailsPort = productId -> {
            calls.add("productDetails:" + productId);
            return products.get(productId);
        };
        GetSimilarProductsService service = new GetSimilarProductsService(
                similarProductIdsPort, productDetailsPort);

        List<ProductDetail> result = service.getSimilarProducts("1");

        assertThat(result).containsExactly(products.get("3"), products.get("2"));
        assertThat(calls).containsExactly("similarIds:1", "productDetails:3", "productDetails:2");
    }

    @Test
    void returnsAnEmptyListWithoutLookingUpDetailsWhenThereAreNoSimilarIds() {
        AtomicInteger detailLookups = new AtomicInteger();
        SimilarProductIdsPort similarProductIdsPort = productId -> List.of();
        ProductDetailsPort productDetailsPort = productId -> {
            detailLookups.incrementAndGet();
            return product("2", "Dress");
        };
        GetSimilarProductsService service = new GetSimilarProductsService(
                similarProductIdsPort, productDetailsPort);

        assertThat(service.getSimilarProducts("1")).isEmpty();
        assertThat(detailLookups).hasValue(0);
    }

    private ProductDetail product(String id, String name) {
        return new ProductDetail(id, name, new BigDecimal("19.99"), true);
    }
}
