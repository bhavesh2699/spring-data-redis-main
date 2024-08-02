package com.javatechie.redis.respository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class RoundRobinService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public Map<Object, Object> initializeCache(String disseminationProfileId, String queueNames) {
    	
    	String[] arr = queueNames.split(","); 
        ArrayList<String> list = new ArrayList<>(Arrays.asList(arr));
        for(String queue: list)
        	redisTemplate.opsForHash().put("ISO_CACHE_DISSEMINATION_ROUNDROBIN_"+disseminationProfileId, queue, queue);
        System.out.println("Data doesn't exist for key's in cache, queues names fetched and added to cache");
        return redisTemplate.opsForHash().entries("ISO_CACHE_DISSEMINATION_ROUNDROBIN_"+disseminationProfileId);

    }


    public String getNextEntry(String disseminationProfileId, Map<Object, Object> cachedQueues) {
    	if(cachedQueues != null) {
	        if (redisTemplate.opsForHash().get("ISO_CACHE_DISSEMINATION_ROUNDROBIN_LAST_ACCESSED_IDX", disseminationProfileId) == null)
	            redisTemplate.opsForHash().put("ISO_CACHE_DISSEMINATION_ROUNDROBIN_LAST_ACCESSED_IDX", disseminationProfileId, -1L);
		        
	        Long counter = redisTemplate.opsForHash().increment("ISO_CACHE_DISSEMINATION_ROUNDROBIN_LAST_ACCESSED_IDX", disseminationProfileId, 1);
	       
	        if (counter >= cachedQueues.size()) {
	            counter = 0L;
	            redisTemplate.opsForHash().put("ISO_CACHE_DISSEMINATION_ROUNDROBIN_LAST_ACCESSED_IDX", disseminationProfileId , counter);
	        }
	        
	        int index = (int) (counter % cachedQueues.size());
	        String nextKey = (String) cachedQueues.keySet().stream().sorted().toArray()[index];
	        
	        return nextKey;
    	}
    	return null;
 
    }
    
}
