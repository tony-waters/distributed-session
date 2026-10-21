package com.distributedsession.session;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
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
}
