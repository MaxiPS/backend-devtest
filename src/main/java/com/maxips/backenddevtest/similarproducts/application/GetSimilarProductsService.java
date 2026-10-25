package com.maxips.backenddevtest.similarproducts.application;

import com.maxips.backenddevtest.similarproducts.domain.ProductDetail;
import com.maxips.backenddevtest.similarproducts.port.in.GetSimilarProductsUseCase;
import com.maxips.backenddevtest.similarproducts.port.out.ProductDetailsPort;
import com.maxips.backenddevtest.similarproducts.port.out.SimilarProductIdsPort;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetSimilarProductsService implements GetSimilarProductsUseCase {

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

        for (String similarId : similarIds) {
            products.add(productDetailsPort.findProductDetails(similarId));
        }

        return List.copyOf(products);
    }
}
