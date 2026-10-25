package com.maxips.backenddevtest.similarproducts.application;

import com.maxips.backenddevtest.similarproducts.application.exception.ProductDetailsUnavailableException;
import com.maxips.backenddevtest.similarproducts.application.exception.RelatedProductNotFoundException;
import com.maxips.backenddevtest.similarproducts.domain.ProductDetail;
import com.maxips.backenddevtest.similarproducts.port.in.GetSimilarProductsUseCase;
import com.maxips.backenddevtest.similarproducts.port.out.ProductDetailsPort;
import com.maxips.backenddevtest.similarproducts.port.out.SimilarProductIdsPort;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GetSimilarProductsService implements GetSimilarProductsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetSimilarProductsService.class);

    private final SimilarProductIdsPort similarProductIdsPort;
    private final ProductDetailsPort productDetailsPort;

    public GetSimilarProductsService(
            SimilarProductIdsPort similarProductIdsPort,
            ProductDetailsPort productDetailsPort) {
        this.similarProductIdsPort = similarProductIdsPort;
        this.productDetailsPort = productDetailsPort;
    }

    @Override
    public List<ProductDetail> getSimilarProducts(String productId) {
        List<String> similarIds = similarProductIdsPort.findSimilarProductIds(productId);
        List<ProductDetail> products = new ArrayList<>(similarIds.size());
        ProductDetailsUnavailableException technicalFailure = null;

        for (String similarId : similarIds) {
            try {
                products.add(productDetailsPort.findProductDetails(similarId));
            } catch (RelatedProductNotFoundException exception) {
                logger.debug("Skipping missing related product {}", similarId);
            } catch (ProductDetailsUnavailableException exception) {
                logger.warn("Could not retrieve details for related product {}: {}", similarId, exception.getMessage());
                if (technicalFailure == null) {
                    technicalFailure = exception;
                }
            }
        }

        if (products.isEmpty() && technicalFailure != null) {
            throw technicalFailure;
        }

        return List.copyOf(products);
    }
}
