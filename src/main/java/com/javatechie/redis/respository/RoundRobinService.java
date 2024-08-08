package com.javatechie.redis.respository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicInteger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class RoundRobinService {

	 	@Autowired
	    private RedisTemplate<String, String> redisTemplate;

	    @Autowired
	    private RedisConnectionFactory redisConnectionFactory;

	    private final Map<String, RedisAtomicInteger> counterCache = new ConcurrentHashMap<>();
	    private final Lock redisTemplateLock = new ReentrantLock();
	    private final Lock counterCacheLock = new ReentrantLock();

	    public List<String> initializeCache(String disseminationProfileId, String queueNames) {
	        ListOperations<String, String> listOps = redisTemplate.opsForList();
	        String[] splitedQueues = queueNames.split(",");
	        List<String> listOfQueues = new ArrayList<>(Arrays.asList(splitedQueues));
	        try {
	            redisTemplateLock.lock();
	            try {
	                listOps.rightPushAll("ISO_CACHE_DISSEMINATION_ROUNDROBIN_" + disseminationProfileId, listOfQueues);
	            } finally {
	                redisTemplateLock.unlock();
	            }
	        } catch (DataAccessException | IndexOutOfBoundsException | ConcurrentModificationException e) {
	            System.out.println("Error initializing cache:" + e);
	            throw new RuntimeException("Error initializing cache", e);
	        } catch (Exception e) {
	            System.out.println("Unexpected error:" + e);
	            throw e;
	        }
	        return listOfQueues;
	    }

	    public String getNextEntry(String disseminationProfileId, List<String> cachedQueues) {
	        if (cachedQueues == null || cachedQueues.isEmpty()) {
	            return "";
	        }

	        String counterKey = "ISO_CACHE_DISSEMINATION_ROUNDROBIN_LAST_ACCESSED_IDX_" + disseminationProfileId;
	        RedisAtomicInteger counter;

	        counterCacheLock.lock();
	        try {
	        	 if (redisTemplate.hasKey(counterKey) == Boolean.FALSE) {
	                 counterCache.remove(counterKey);
	             }
	            counter = counterCache.computeIfAbsent(counterKey, k -> new RedisAtomicInteger(k, redisConnectionFactory));
	            //System.out.println("counter: " + counter);
	        } finally {
	            counterCacheLock.unlock();
	        }

	        try {
	            counterCacheLock.lock();
	            try {
	                int index = counter.getAndIncrement() % cachedQueues.size();
	                if (counter.get() >= (Integer.MAX_VALUE / 2)) {
	                    counter.compareAndSet(counter.get(), 0);
	                }
	                return cachedQueues.get(index);
	            } finally {
	                counterCacheLock.unlock();
	            }
	        } catch (DataAccessException | IndexOutOfBoundsException | ConcurrentModificationException e) {
	            System.out.println("Error getting next entry:" + e);
	            throw new RuntimeException("Error getting next entry", e);
	        } catch (Exception e) {
	            System.out.println("Unexpected error:" + e);
	            throw e;
	        }
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
