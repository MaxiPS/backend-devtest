package com.maxips.backenddevtest.similarproducts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maxips.backenddevtest.similarproducts.application.exception.ProductDetailsUnavailableException;
import com.maxips.backenddevtest.similarproducts.application.exception.RelatedProductNotFoundException;
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

    @Test
    void omitsFailedRelatedProductsWhenOtherDetailsAreAvailable() {
        ProductDetail second = product("2", "Dress");
        ProductDetail third = product("3", "Blazer");
        SimilarProductIdsPort similarProductIdsPort = productId -> List.of("missing", "unavailable", "2", "3");
        ProductDetailsPort productDetailsPort = productId -> switch (productId) {
            case "missing" -> throw new RelatedProductNotFoundException(productId);
            case "unavailable" -> throw new ProductDetailsUnavailableException(productId, 503);
            case "2" -> second;
            case "3" -> third;
            default -> throw new IllegalArgumentException("Unexpected product ID: " + productId);
        };
        GetSimilarProductsService service = new GetSimilarProductsService(
                similarProductIdsPort, productDetailsPort);

        assertThat(service.getSimilarProducts("1")).containsExactly(second, third);
    }

    @Test
    void returnsAnEmptyListWhenEveryRelatedProductIsNotFound() {
        SimilarProductIdsPort similarProductIdsPort = productId -> List.of("missing-1", "missing-2");
        ProductDetailsPort productDetailsPort = productId -> {
            throw new RelatedProductNotFoundException(productId);
        };
        GetSimilarProductsService service = new GetSimilarProductsService(
                similarProductIdsPort, productDetailsPort);

        assertThat(service.getSimilarProducts("1")).isEmpty();
    }

    @Test
    void failsWhenNoProductIsAvailableAndAtLeastOneLookupFailsTechnically() {
        ProductDetailsUnavailableException unavailable = new ProductDetailsUnavailableException("unavailable", 503);
        SimilarProductIdsPort similarProductIdsPort = productId -> List.of("missing", "unavailable");
        ProductDetailsPort productDetailsPort = productId -> {
            if (productId.equals("missing")) {
                throw new RelatedProductNotFoundException(productId);
            }
            throw unavailable;
        };
        GetSimilarProductsService service = new GetSimilarProductsService(
                similarProductIdsPort, productDetailsPort);

        assertThatThrownBy(() -> service.getSimilarProducts("1")).isSameAs(unavailable);
    }

    private ProductDetail product(String id, String name) {
        return new ProductDetail(id, name, new BigDecimal("19.99"), true);
    }
}
