package com.reliaquest.api.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for HTTP client beans.
 * Sets up RestTemplate with appropriate timeouts and connection pooling.
 */
@Configuration
@EnableRetry
public class HttpClientConfig {

    /**
     * Creates a configured RestTemplate bean for making HTTP requests.
     * Includes connection and read timeouts for resilience.
     *
     * @param builder RestTemplateBuilder provided by Spring Boot
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .requestFactory(this::clientHttpRequestFactory)
                .build();
    }

    /**
     * Creates a ClientHttpRequestFactory with custom timeout settings.
     *
     * @return configured ClientHttpRequestFactory
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 sec
        factory.setReadTimeout(30000); // 30 sec
        return factory;
    }
}
