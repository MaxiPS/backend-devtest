package com.maxips.backenddevtest.similarproducts.application;

import com.maxips.backenddevtest.similarproducts.application.exception.ProductDetailsUnavailableException;
import com.maxips.backenddevtest.similarproducts.application.exception.ProductLookupOverloadedException;
import com.maxips.backenddevtest.similarproducts.application.exception.RelatedProductNotFoundException;
import com.maxips.backenddevtest.similarproducts.domain.ProductDetail;
import com.maxips.backenddevtest.similarproducts.port.in.GetSimilarProductsUseCase;
import com.maxips.backenddevtest.similarproducts.port.out.ProductDetailsPort;
import com.maxips.backenddevtest.similarproducts.port.out.SimilarProductIdsPort;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetSimilarProductsService implements GetSimilarProductsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetSimilarProductsService.class);

    private final SimilarProductIdsPort similarProductIdsPort;
    private final ProductDetailsPort productDetailsPort;
    private final Executor productLookupExecutor;

    public GetSimilarProductsService(
            SimilarProductIdsPort similarProductIdsPort,
            ProductDetailsPort productDetailsPort,
            Executor productLookupExecutor) {
        this.similarProductIdsPort = similarProductIdsPort;
        this.productDetailsPort = productDetailsPort;
        this.productLookupExecutor = productLookupExecutor;
    }

    @Override
    public List<ProductDetail> getSimilarProducts(String productId) {
        List<String> similarIds = similarProductIdsPort.findSimilarProductIds(productId);
        List<ProductDetail> products = new ArrayList<>(similarIds.size());
        ProductDetailsUnavailableException technicalFailure = null;
        ProductLookupOverloadedException overloadFailure = null;
        List<CompletableFuture<ProductDetail>> lookups = new ArrayList<>(similarIds.size());

        for (String similarId : similarIds) {
            try {
                lookups.add(CompletableFuture.supplyAsync(
                        () -> productDetailsPort.findProductDetails(similarId), productLookupExecutor));
            } catch (RejectedExecutionException exception) {
                lookups.add(CompletableFuture.failedFuture(
                        new ProductLookupOverloadedException(similarId, exception)));
            }
        }

        for (int index = 0; index < lookups.size(); index++) {
            String similarId = similarIds.get(index);
            try {
                products.add(lookups.get(index).join());
            } catch (CompletionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RelatedProductNotFoundException) {
                    logger.debug("Skipping missing related product {}", similarId);
                } else if (cause instanceof ProductDetailsUnavailableException unavailableException) {
                    logger.warn("Could not retrieve details for related product {}: {}", similarId, cause.getMessage());
                    if (technicalFailure == null) {
                        technicalFailure = unavailableException;
                    }
                } else if (cause instanceof ProductLookupOverloadedException overloadedException) {
                    logger.warn("Lookup capacity exceeded for related product {}", similarId);
                    if (overloadFailure == null) {
                        overloadFailure = overloadedException;
                    }
                } else if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else {
                    throw exception;
                }
            }
        }

        if (products.isEmpty()) {
            if (overloadFailure != null) {
                throw overloadFailure;
            }
            if (technicalFailure != null) {
                throw technicalFailure;
            }
        }

        return List.copyOf(products);
    }
}
