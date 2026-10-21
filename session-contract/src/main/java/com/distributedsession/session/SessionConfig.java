package com.distributedsession.session;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

// Shared by login-service and cart-service via auto-configuration import (see
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
// so both agree on one serializer: Spring Session's default (JDK serialization)
// can't be deserialized across two separate services' JVMs/classpaths.
@AutoConfiguration
public class SessionConfig {

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return GenericJacksonJsonRedisSerializer.builder().build();
    }
}
