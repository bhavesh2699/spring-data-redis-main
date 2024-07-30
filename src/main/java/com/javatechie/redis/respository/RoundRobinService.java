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

    private AtomicInteger counter;
    
    String queue = "q1,q2,q3,q4,q5";// for tesing, will need to pass values from DB

    public Map.Entry<String, String> getNextEntry(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        counter = new AtomicInteger(-1);

        if (entries == null || entries.isEmpty()) {
        	String[] arr = queue.split(","); 
            ArrayList<String> list = new ArrayList<>(Arrays.asList(arr));
            for(String queue: list)
            	redisTemplate.opsForHash().put(key, queue, queue);
            System.out.println("Data doesn't exist for key's in cache, queues names fetched and added to cache");
            entries = redisTemplate.opsForHash().entries(key);
        }
        
        if(redisTemplate.opsForHash().entries("currentCount").containsKey(key)) {
        	int k = Integer.parseInt(redisTemplate.opsForHash().entries("currentCount").get(key).toString());
        	counter.set(k);
        }
        
        //System.out.println("counter::"+ counter);
        int index = counter.incrementAndGet() % entries.size();
        //System.out.println("counter.counter.get()()::"+ counter.get());
        //System.out.println("index::"+ index);
        //System.out.println("================================");
        String nextKey = (String) entries.keySet().stream().sorted().toArray()[index];
        String nextValue = (String) entries.get(nextKey);
        redisTemplate.opsForHash().put("currentCount", key ,index);

        return Map.entry(nextKey, nextValue);
    }
    
    /*public void addValue(String key, String value) {
    	String[] arr = value.split(","); 
        ArrayList<String> list = new ArrayList<>(Arrays.asList(arr));
        for(String queue: list)
        	redisTemplate.opsForHash().put(key, queue, queue);
    }*/
}
