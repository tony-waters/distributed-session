package com.distributedsession.cartservice.cart;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<SessionAuthenticationFilter> sessionAuthenticationFilter(
            ObjectMapper objectMapper) {
        FilterRegistrationBean<SessionAuthenticationFilter> registration = new FilterRegistrationBean<>(
                new SessionAuthenticationFilter(objectMapper));
        registration.addUrlPatterns("/cart", "/cart/*");
        return registration;
    }
}
