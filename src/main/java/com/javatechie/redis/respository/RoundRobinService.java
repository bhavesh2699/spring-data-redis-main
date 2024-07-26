package com.javatechie.redis.respository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RoundRobinService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private AtomicInteger counter = new AtomicInteger(0);

    public Map.Entry<String, String> getNextEntry(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries == null || entries.isEmpty()) {
            return null;
        }
        
        int index = counter.getAndIncrement() % entries.size();
        String nextKey = (String) entries.keySet().stream().sorted().toArray()[index];
        String nextValue = (String) entries.get(nextKey);

        return Map.entry(nextKey, nextValue);
    }
    
    public void addValue(String key, String value) {
    	String[] arr = value.split(","); 
        ArrayList<String> list = new ArrayList<>(Arrays.asList(arr));
        for(String queue: list)
        	redisTemplate.opsForHash().put(key, queue, queue);
    }
}
