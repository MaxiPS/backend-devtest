package com.maxips.backenddevtest.similarproducts.adapter.out.http;

import java.math.BigDecimal;

record ProductDetailsResponse(String id, String name, BigDecimal price, Boolean availability) {}
