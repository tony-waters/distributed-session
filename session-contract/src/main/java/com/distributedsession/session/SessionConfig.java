package com.distributedsession.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

// Shared by login-service and cart-service via auto-configuration import (see
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
// so both agree on one serializer: Spring Session's default (JDK serialization)
// can't be deserialized across two separate services' JVMs/classpaths.
//
// @EnableRedisHttpSession is required here because this Spring Boot version ships
// no auto-configuration of its own for Spring Session Redis (unlike older Boot
// versions) - both services need this to get a Redis-backed HttpSession at all.
@AutoConfiguration
@EnableRedisHttpSession(redisNamespace = "${spring.session.redis.namespace:spring:session}")
public class SessionConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return GenericJacksonJsonRedisSerializer.builder().build();
    }

    // Shared by both services so the frontend reads one header name everywhere. Highest
    // precedence and an unscoped "/*" so it also labels /actuator/health and the 401s
    // cart-service's SessionAuthenticationFilter writes - unlike that filter, adding a
    // response header can't reject anything, so the health probe stays unaffected.
    @Bean
    public FilterRegistrationBean<InstanceIdFilter> instanceIdFilter(
            @Value("${INSTANCE_ID:unknown}") String instanceId) {
        FilterRegistrationBean<InstanceIdFilter> registration =
                new FilterRegistrationBean<>(new InstanceIdFilter(instanceId));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
