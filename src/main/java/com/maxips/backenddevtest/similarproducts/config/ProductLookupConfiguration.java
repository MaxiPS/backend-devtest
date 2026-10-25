package com.maxips.backenddevtest.similarproducts.config;

import com.maxips.backenddevtest.similarproducts.application.GetSimilarProductsService;
import com.maxips.backenddevtest.similarproducts.port.in.GetSimilarProductsUseCase;
import com.maxips.backenddevtest.similarproducts.port.out.ProductDetailsPort;
import com.maxips.backenddevtest.similarproducts.port.out.SimilarProductIdsPort;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
public class ProductLookupConfiguration {

    @Bean(name = "productLookupExecutor")
    ThreadPoolTaskExecutor productLookupExecutor(
            @Value("${product-api.max-concurrent-lookups}") int maxConcurrentLookups,
            @Value("${product-api.lookup-queue-capacity}") int lookupQueueCapacity) {
        if (maxConcurrentLookups < 1 || lookupQueueCapacity < 1) {
            throw new IllegalArgumentException("Product lookup concurrency and queue capacity must be positive");
        }

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(maxConcurrentLookups);
        executor.setMaxPoolSize(maxConcurrentLookups);
        executor.setQueueCapacity(lookupQueueCapacity);
        executor.setThreadNamePrefix("product-lookup-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }

    @Bean
    GetSimilarProductsUseCase getSimilarProductsUseCase(
            SimilarProductIdsPort similarProductIdsPort,
            ProductDetailsPort productDetailsPort,
            @Qualifier("productLookupExecutor") Executor productLookupExecutor) {
        return new GetSimilarProductsService(similarProductIdsPort, productDetailsPort, productLookupExecutor);
    }
}
