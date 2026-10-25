package com.maxips.backenddevtest.similarproducts.adapter.out.http;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class ProductApiRestClientConfiguration {

    @Bean
    RestClient productApiRestClient(
            RestClient.Builder builder,
            @Value("${product-api.base-url}") String baseUrl,
            @Value("${product-api.connect-timeout}") Duration connectTimeout,
            @Value("${product-api.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return builder.baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
