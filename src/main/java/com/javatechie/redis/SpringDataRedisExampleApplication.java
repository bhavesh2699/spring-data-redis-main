package com.javatechie.redis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.javatechie.redis.respository.RoundRobinService;

@SpringBootApplication
@RestController
@RequestMapping("/api/v1")
public class SpringDataRedisExampleApplication {
	
	@Autowired
    private RoundRobinService roundRobinService;
	
	@Autowired
    private RedisTemplate<String, String> redisTemplate;
	
    private final Lock lock = new ReentrantLock();
	
	String queueNames = "q1,q2,q3,q4,q5,q6"; //for testing, will need to pass values from DB

	@GetMapping("/getNextEntry")
	public String getNextEntry(@RequestParam String disseminationProfileId) {
	    lock.lock();
	    try {
	        ListOperations<String, String> listOps = redisTemplate.opsForList();
	        List<String> cachedQueues = listOps.range("ISO_CACHE_DISSEMINATION_ROUNDROBIN_" + disseminationProfileId, 0, -1);

	        String[] splitedQueues = queueNames.split(",");
	        List<String> listOfQueues = new ArrayList<>(Arrays.asList(splitedQueues));

	        if (cachedQueues == null || cachedQueues.isEmpty() || !deleteCacheIfNecessary(disseminationProfileId, cachedQueues, listOfQueues)) {
	            resetLastAccessedIdx(disseminationProfileId); // reset counter if queues don't exist
	            cachedQueues = roundRobinService.initializeCache(disseminationProfileId, queueNames);
	        }
	        return roundRobinService.getNextEntry(disseminationProfileId, cachedQueues);
	    }catch (DataAccessException e) {
            // Handle Redis-specific exceptions
	    	System.out.println("Error accessing Redis cache:" + e);
            throw new RuntimeException("Error accessing Redis cache", e); // Or rethrow a custom exception
        }
	    catch (Exception e) {
	        // Handle general exceptions
            System.out.println("Error retrieving next entry" + e);
	        throw new RuntimeException("Error retrieving next entry", e);
	    }  finally {
	        lock.unlock();
	    }
	}

	private Boolean deleteCacheIfNecessary(String disseminationProfileId, List<String> cachedQueues, List<String> listOfQueues) {
	    lock.lock();
	    try {
	        Boolean compareQueueNames = compareQueueNames(cachedQueues, listOfQueues);
	        if (!compareQueueNames) {
	            // Delete the cache
	            System.out.println("cache Deleted...");
	            redisTemplate.delete("ISO_CACHE_DISSEMINATION_ROUNDROBIN_" + disseminationProfileId);
	        }
	        return compareQueueNames;
	    } catch (DataAccessException e) {
            // Handle Redis-specific exceptions
	    	System.out.println("Error accessing Redis cache:" + e);
            throw new RuntimeException("Error accessing Redis cache", e); // Or rethrow a custom exception
        }
	    catch (Exception e) {
	        // Handle general exceptions
            System.out.println("Error retrieving next entry" + e);
	        throw new RuntimeException("Error retrieving next entry", e);
	    }  finally {
	        lock.unlock();
	    }
	}

	private void resetLastAccessedIdx(String disseminationProfileId) {
	    lock.lock();
	    try {
	        String counterKey = "ISO_CACHE_DISSEMINATION_ROUNDROBIN_LAST_ACCESSED_IDX_" + disseminationProfileId;
	        if (redisTemplate.hasKey(counterKey)) {
	            System.out.println("cache reset...");
	            redisTemplate.opsForValue().set(counterKey, "0");
	        }
	    } catch (DataAccessException e) {
            // Handle Redis-specific exceptions
	    	System.out.println("Error accessing Redis cache:" + e);
            throw new RuntimeException("Error accessing Redis cache", e); // Or rethrow a custom exception
        }
	    catch (Exception e) {
	        // Handle general exceptions
            System.out.println("Error retrieving next entry" + e);
	        throw new RuntimeException("Error retrieving next entry", e);
	    }  finally {
	        lock.unlock();
	    }
	}

	private Boolean compareQueueNames(List<String> cachedQueues, List<String> listOfQueues) {
	    System.out.println("cache compare...");
	    return cachedQueues.equals(listOfQueues);
	}

    
    
    /*
    @GetMapping("/getNextEntry")
    public String getNextEntry(@RequestParam String disseminationProfileId) {
    	Map<Object, Object> cachedQueues = redisTemplate.opsForHash().entries("ISO_CACHE_DISSEMINATION_ROUNDROBIN_"+disseminationProfileId);
    	if(!cachedQueues.isEmpty()) 
    		return roundRobinService.getNextEntry(disseminationProfileId, cachedQueues);
    	else {
            System.out.println("Cache is NULL/Empty "+cachedQueues);
            cachedQueues = roundRobinService.initializeCache(disseminationProfileId, queueNames);
            return roundRobinService.getNextEntry(disseminationProfileId, cachedQueues);
    	}
    }*/
    
    /*@PostMapping("/add")
    public ResponseEntity<Void> addValue(@RequestParam String key, @RequestParam String value) {
        roundRobinService.addValue(key, value);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }*/
    
    public static void main(String[] args) {
        SpringApplication.run(SpringDataRedisExampleApplication.class, args);
    }

}
