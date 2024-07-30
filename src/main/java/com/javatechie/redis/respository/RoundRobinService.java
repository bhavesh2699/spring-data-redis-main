package com.javatechie.redis.respository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RoundRobinService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    //private AtomicInteger counter;

    public Map.Entry<String, String> getNextEntry(String disseminationProfileId, String queueNames) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(disseminationProfileId);
        //counter = new AtomicInteger(-1);

        if (entries == null || entries.isEmpty()) {
        	String[] arr = queueNames.split(","); 
            ArrayList<String> list = new ArrayList<>(Arrays.asList(arr));
            for(String queue: list)
            	redisTemplate.opsForHash().put(disseminationProfileId, queue, String.valueOf(System.currentTimeMillis())); //store queue names with current time
            System.out.println("Data doesn't exist for key's in cache, queues names fetched and added to cache");
            entries = redisTemplate.opsForHash().entries(disseminationProfileId);
        }
        
        //logic to fetch queues which is least recently used for particular dissemination profile id
        Entry<Object, Object> lruEntry = Collections.min(
        		entries.entrySet(),
                Comparator.comparingLong(entry -> Long.parseLong((String) entry.getValue()))
            );

        String lruQueueKey = lruEntry.getKey().toString();
        String lruQueueVal = lruEntry.getValue().toString();
        
        redisTemplate.opsForHash().put(disseminationProfileId, lruQueueKey, String.valueOf(System.currentTimeMillis())); ////update queue names with current time

      
        return Map.entry(lruQueueKey, lruQueueVal);
    }
    
    /*public void addValue(String key, String value) {
    	String[] arr = value.split(","); 
        ArrayList<String> list = new ArrayList<>(Arrays.asList(arr));
        for(String queue: list)
        	redisTemplate.opsForHash().put(key, queue, queue);
    }*/
}
