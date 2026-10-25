package com.maxips.backenddevtest.similarproducts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.maxips.backenddevtest.similarproducts.application.exception.ProductDetailsUnavailableException;
import com.maxips.backenddevtest.similarproducts.application.exception.ProductLookupOverloadedException;
import com.maxips.backenddevtest.similarproducts.application.exception.RelatedProductNotFoundException;
import com.maxips.backenddevtest.similarproducts.domain.ProductDetail;
import com.maxips.backenddevtest.similarproducts.port.out.ProductDetailsPort;
import com.maxips.backenddevtest.similarproducts.port.out.SimilarProductIdsPort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GetSimilarProductsServiceTest {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

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
                similarProductIdsPort, productDetailsPort, DIRECT_EXECUTOR);

        List<ProductDetail> result = service.getSimilarProducts("1");

        assertThat(result).containsExactly(products.get("3"), products.get("2"));
        assertThat(calls).containsExactly("similarIds:1", "productDetails:3", "productDetails:2");
    }

    @Test
    void looksUpEachSimilarIdOnceAndPreservesTheFirstOccurrenceOrder() {
        List<String> detailLookups = new ArrayList<>();
        SimilarProductIdsPort similarProductIdsPort = productId -> List.of("3", "2", "3", "2");
        ProductDetailsPort productDetailsPort = productId -> {
            detailLookups.add(productId);
            return product(productId, "Product " + productId);
        };
        GetSimilarProductsService service = new GetSimilarProductsService(
                similarProductIdsPort, productDetailsPort, DIRECT_EXECUTOR);

        List<ProductDetail> result = service.getSimilarProducts("1");

        assertThat(detailLookups).containsExactly("3", "2");
        assertThat(result).containsExactly(product("3", "Product 3"), product("2", "Product 2"));
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
                similarProductIdsPort, productDetailsPort, DIRECT_EXECUTOR);

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
                similarProductIdsPort, productDetailsPort, DIRECT_EXECUTOR);

        assertThat(service.getSimilarProducts("1")).containsExactly(second, third);
    }

    @Test
    void returnsAnEmptyListWhenEveryRelatedProductIsNotFound() {
        SimilarProductIdsPort similarProductIdsPort = productId -> List.of("missing-1", "missing-2");
        ProductDetailsPort productDetailsPort = productId -> {
            throw new RelatedProductNotFoundException(productId);
        };
        GetSimilarProductsService service = new GetSimilarProductsService(
                similarProductIdsPort, productDetailsPort, DIRECT_EXECUTOR);

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
                similarProductIdsPort, productDetailsPort, DIRECT_EXECUTOR);

        assertThatThrownBy(() -> service.getSimilarProducts("1")).isSameAs(unavailable);
    }

    @Test
    void performsLookupsConcurrentlyAndPreservesTheSimilarIdOrder() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstLookupStarted = new CountDownLatch(1);
        CountDownLatch otherLookupsCompleted = new CountDownLatch(2);
        AtomicInteger activeLookups = new AtomicInteger();
        AtomicInteger maximumActiveLookups = new AtomicInteger();
        SimilarProductIdsPort similarProductIdsPort = productId -> List.of("first", "second", "third");
        ProductDetailsPort productDetailsPort = productId -> {
            int active = activeLookups.incrementAndGet();
            maximumActiveLookups.accumulateAndGet(active, Math::max);
            try {
                if (productId.equals("first")) {
                    firstLookupStarted.countDown();
                    if (!otherLookupsCompleted.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Other lookups did not run concurrently");
                    }
                } else {
                    if (!firstLookupStarted.await(2, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("First lookup did not start");
                    }
                    otherLookupsCompleted.countDown();
                }
                return product(productId, productId);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            } finally {
                activeLookups.decrementAndGet();
            }
        };
        GetSimilarProductsService service = new GetSimilarProductsService(
                similarProductIdsPort, productDetailsPort, executor);

        try {
            assertThat(service.getSimilarProducts("1")).containsExactly(
                    product("first", "first"),
                    product("second", "second"),
                    product("third", "third"));
            assertThat(maximumActiveLookups).hasValue(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failsWithAnOverloadExceptionWhenTheLookupExecutorRejectsEveryTask() {
        SimilarProductIdsPort similarProductIdsPort = productId -> List.of("2", "3");
        ProductDetailsPort productDetailsPort = productId -> product(productId, "Product " + productId);
        Executor rejectingExecutor = command -> {
            throw new RejectedExecutionException("Lookup executor is saturated");
        };
        GetSimilarProductsService service = new GetSimilarProductsService(
                similarProductIdsPort, productDetailsPort, rejectingExecutor);

        assertThatThrownBy(() -> service.getSimilarProducts("1"))
                .isInstanceOf(ProductLookupOverloadedException.class);
    }

    @Test
    void keepsSuccessfulDetailsWhenTheLookupExecutorRejectsAnotherTask() {
        SimilarProductIdsPort similarProductIdsPort = productId -> List.of("2", "3");
        ProductDetailsPort productDetailsPort = productId -> product(productId, "Product " + productId);
        AtomicInteger submittedTasks = new AtomicInteger();
        Executor executor = command -> {
            if (submittedTasks.getAndIncrement() == 0) {
                command.run();
            } else {
                throw new RejectedExecutionException("Lookup executor is saturated");
            }
        };
        GetSimilarProductsService service = new GetSimilarProductsService(
                similarProductIdsPort, productDetailsPort, executor);

        assertThat(service.getSimilarProducts("1"))
                .containsExactly(product("2", "Product 2"));
    }

    private ProductDetail product(String id, String name) {
        return new ProductDetail(id, name, new BigDecimal("19.99"), true);
    }
}
