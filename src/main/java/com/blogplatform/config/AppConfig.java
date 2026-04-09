package com.blogplatform.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
@EnableCaching
public class AppConfig {
    // Async, Scheduling, and Caching enabled via annotations.
    // Additional beans (thread pools, cache managers) can be configured here.
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory f) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setConnectionFactory(f);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        return t;
    }
}
