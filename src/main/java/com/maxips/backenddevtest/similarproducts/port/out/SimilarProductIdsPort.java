package com.maxips.backenddevtest.similarproducts.port.out;

import java.util.List;

public interface SimilarProductIdsPort {

    List<String> findSimilarProductIds(String productId);
}
