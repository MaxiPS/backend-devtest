package com.maxips.backenddevtest.similarproducts.adapter.in.web;

import com.maxips.backenddevtest.similarproducts.application.exception.ProductDetailsUnavailableException;
import com.maxips.backenddevtest.similarproducts.application.exception.ProductLookupOverloadedException;
import com.maxips.backenddevtest.similarproducts.application.exception.ProductNotFoundException;
import com.maxips.backenddevtest.similarproducts.application.exception.SimilarProductIdsUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<Void> handleProductNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({ProductDetailsUnavailableException.class, SimilarProductIdsUnavailableException.class})
    ResponseEntity<Void> handleDependencyUnavailable() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }

    @ExceptionHandler(ProductLookupOverloadedException.class)
    ResponseEntity<Void> handleProductLookupOverloaded() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
