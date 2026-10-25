package com.maxips.backenddevtest.similarproducts.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.maxips.backenddevtest.similarproducts.application.exception.ProductDetailsUnavailableException;
import com.maxips.backenddevtest.similarproducts.application.exception.ProductLookupOverloadedException;
import com.maxips.backenddevtest.similarproducts.application.exception.ProductNotFoundException;
import com.maxips.backenddevtest.similarproducts.application.exception.SimilarProductIdsUnavailableException;
import com.maxips.backenddevtest.similarproducts.domain.ProductDetail;
import com.maxips.backenddevtest.similarproducts.port.in.GetSimilarProductsUseCase;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SimilarProductsController.class)
class SimilarProductsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSimilarProductsUseCase getSimilarProductsUseCase;

    @Test
    void returnsSimilarProductDetailsUsingThePublicApiContract() throws Exception {
        when(getSimilarProductsUseCase.getSimilarProducts("1")).thenReturn(List.of(
                product("2", "Dress", "19.99", true),
                product("3", "Blazer", "29.99", false)));

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("2"))
                .andExpect(jsonPath("$[0].name").value("Dress"))
                .andExpect(jsonPath("$[0].price").value(19.99))
                .andExpect(jsonPath("$[0].availability").value(true))
                .andExpect(jsonPath("$[1].id").value("3"))
                .andExpect(jsonPath("$[1].availability").value(false));

        verify(getSimilarProductsUseCase).getSimilarProducts("1");
    }

    @Test
    void returnsAnEmptyJsonArrayWhenThereAreNoSimilarProducts() throws Exception {
        when(getSimilarProductsUseCase.getSimilarProducts("1")).thenReturn(List.of());

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void mapsAProductNotFoundExceptionToNotFound() throws Exception {
        when(getSimilarProductsUseCase.getSimilarProducts("missing"))
                .thenThrow(new ProductNotFoundException("missing"));

        mockMvc.perform(get("/product/missing/similar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void mapsSimilarIdsServiceFailureToBadGateway() throws Exception {
        when(getSimilarProductsUseCase.getSimilarProducts("1"))
                .thenThrow(new SimilarProductIdsUnavailableException("1", 503));

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void mapsProductDetailsServiceFailureToBadGateway() throws Exception {
        when(getSimilarProductsUseCase.getSimilarProducts("1"))
                .thenThrow(new ProductDetailsUnavailableException("2", 503));

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void mapsLookupExecutorSaturationToServiceUnavailable() throws Exception {
        when(getSimilarProductsUseCase.getSimilarProducts("1"))
                .thenThrow(new ProductLookupOverloadedException("2"));

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isServiceUnavailable());
    }

    private ProductDetail product(String id, String name, String price, boolean availability) {
        return new ProductDetail(id, name, new BigDecimal(price), availability);
    }
}
