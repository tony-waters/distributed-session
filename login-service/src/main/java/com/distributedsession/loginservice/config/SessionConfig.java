package com.distributedsession.loginservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class SessionConfig {

    // Spring Session's default (JDK serialization) can't be deserialized across two
    // separate services' JVMs/classpaths. cart-service must declare this exact bean
    // (name and type) so both sides agree on how session attributes are encoded in Redis.
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return GenericJacksonJsonRedisSerializer.builder().build();
    }
}
