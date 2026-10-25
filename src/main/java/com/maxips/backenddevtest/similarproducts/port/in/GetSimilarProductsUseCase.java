package com.maxips.backenddevtest.similarproducts.port.in;

import com.maxips.backenddevtest.similarproducts.domain.ProductDetail;
import java.util.List;

public interface GetSimilarProductsUseCase {

    List<ProductDetail> getSimilarProducts(String productId);
}
