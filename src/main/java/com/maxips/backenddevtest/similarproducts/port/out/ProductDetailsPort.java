package com.maxips.backenddevtest.similarproducts.port.out;

import com.maxips.backenddevtest.similarproducts.domain.ProductDetail;

public interface ProductDetailsPort {

    ProductDetail findProductDetails(String productId);
}
