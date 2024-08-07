package com.javatechie.redis.respository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicInteger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoundRobinService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    
    public List<String> initializeCache(String disseminationProfileId, List<String> listOfQueues) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        listOps.rightPushAll("ISO_CACHE_DISSEMINATION_ROUNDROBIN_" + disseminationProfileId, listOfQueues);
        return listOfQueues;
    }


    public String getNextEntry(String disseminationProfileId, List<String> cachedQueues) {
    	 if (cachedQueues != null && !cachedQueues.isEmpty()) {
             String counterKey = "ISO_CACHE_DISSEMINATION_ROUNDROBIN_LAST_ACCESSED_IDX_" + disseminationProfileId;
             RedisAtomicInteger counter = new RedisAtomicInteger(counterKey, redisConnectionFactory);
             System.out.println("counter: "+counter);
             int index = counter.getAndIncrement() % cachedQueues.size();
             if(counter.get() >= 20) 
            	 counter.set(0); // Reset counter to prevent overflow

             return cachedQueues.get(index);
         }
    	return "";
 
    }
    
    /*public Map<Object, Object> initializeCache(String disseminationProfileId, String queueNames) {
    	
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
 
    }*/
    
}
