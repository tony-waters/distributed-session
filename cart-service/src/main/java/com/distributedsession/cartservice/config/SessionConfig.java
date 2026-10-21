package com.distributedsession.cartservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class SessionConfig {

    // Must match login-service's bean exactly: without a shared JSON serializer,
    // cart-service can't deserialize session attributes login-service wrote
    // (Spring Session's default JDK serialization doesn't work across JVMs/classpaths).
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return GenericJacksonJsonRedisSerializer.builder().build();
    }
}
