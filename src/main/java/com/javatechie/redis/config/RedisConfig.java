package com.javatechie.redis.config;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
@EnableRedisRepositories
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {

    @Bean
    public JedisConnectionFactory connectionFactory()  {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName("localhost");
        configuration.setPort(6379);
        
        return new JedisConnectionFactory(configuration);
    }

    @Bean
    public RedisTemplate<String, Object> template() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        //template.setHashKeySerializer(new StringRedisSerializer());
        //template.setHashKeySerializer(new JdkSerializationRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        //template.setHashKeySerializer(new StringRedisSerializer(StandardCharsets.UTF_8));
        //template.setHashValueSerializer(new StringRedisSerializer(StandardCharsets.UTF_8));
        //template.setKeySerializer(new StringRedisSerializer());
        //template.setValueSerializer(new GenericToStringSerializer<>(Long.class)); // Use for integer values
        
        // Set hash key and hash value serializers
        //template.setHashKeySerializer(new StringRedisSerializer());
        //template.setHashValueSerializer(new GenericToStringSerializer<>(String.class));
        return template;
    }
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues(); // Optional: disable caching of null values

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(cacheConfig)
            .build();
    }

}