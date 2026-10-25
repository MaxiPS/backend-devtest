package com.maxips.backenddevtest.similarproducts.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "product-api.max-concurrent-lookups=3",
            "product-api.lookup-queue-capacity=17"
        })
class ProductLookupExecutorConfigurationTest {

    @Autowired
    @Qualifier("productLookupExecutor")
    private ThreadPoolTaskExecutor productLookupExecutor;

    @Test
    void usesTheConfiguredConcurrencyAndQueueBounds() {
        assertThat(productLookupExecutor.getCorePoolSize()).isEqualTo(3);
        assertThat(productLookupExecutor.getMaxPoolSize()).isEqualTo(3);
        assertThat(productLookupExecutor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(17);
    }
}
