package com.javatechie.redis;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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
		
	String queueNames = "q1,q2,q3,q4,q5,q6"; //for testing, will need to pass values from DB
	
	private final Lock javaLock = new ReentrantLock();

    private String generateLockKey(String disseminationProfileId) {
        return "ISO_CACHE_DISSEMINATION_ROUNDROBIN::ISO_CACHE_DISSEMINATION_ROUNDROBIN_LOCK_" + disseminationProfileId;
    }

    @GetMapping("/getNextEntry")
    public String getNextEntry(@RequestParam String disseminationProfileId) {
        String lockKey = generateLockKey(disseminationProfileId);
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        ListOperations<String, String> listOps = redisTemplate.opsForList();

        try {

            List<String> cachedQueues = listOps.range("ISO_CACHE_DISSEMINATION_ROUNDROBIN::ISO_CACHE_DISSEMINATION_ROUNDROBIN_" + disseminationProfileId, 0, -1);

            while (cachedQueues == null || cachedQueues.isEmpty()) {
                boolean redisLockAcquired = false;

                try {
                    javaLock.lock();  // Acquire the Java lock to synchronize threads within the same JVM

                    // Try to acquire the Redis lock with a timeout
                    redisLockAcquired = Boolean.TRUE.equals(valueOps.setIfAbsent(lockKey, "locked"));

                    if (redisLockAcquired) {
                        // Recheck after acquiring the lock to avoid redundant initialization
                        cachedQueues = listOps.range("ISO_CACHE_DISSEMINATION_ROUNDROBIN::ISO_CACHE_DISSEMINATION_ROUNDROBIN_" + disseminationProfileId, 0, -1);
                        if (cachedQueues == null || cachedQueues.isEmpty()) {
                            // Initialize cache since it is still empty
                            cachedQueues = roundRobinService.initializeCache(disseminationProfileId, queueNames);
                        }
                    } else {
                        // Wait and retry if the Redis lock was not acquired
                        Thread.sleep(100);  // Sleep before retrying to acquire the lock
                        cachedQueues = listOps.range("ISO_CACHE_DISSEMINATION_ROUNDROBIN::ISO_CACHE_DISSEMINATION_ROUNDROBIN_" + disseminationProfileId, 0, -1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while trying to acquire lock", e);
                } catch (Exception e) {
                    System.out.println("Error retrieving next entry: " + e);
                    throw new RuntimeException("Error retrieving next entry", e);
                }
                finally {
                    if (redisLockAcquired) {
                        redisTemplate.delete(lockKey);  // Release the Redis lock
                    }
                    javaLock.unlock();  // Release the Java lock
                }
            }

            // Return the next entry from the initialized cache
            return roundRobinService.getNextEntry(disseminationProfileId, cachedQueues);

        } catch (DataAccessException e) {
            System.out.println("Error accessing Redis cache: " + e);
            throw new RuntimeException("Error accessing Redis cache", e);
        } catch (Exception e) {
            System.out.println("Error retrieving next entry: " + e);
            throw new RuntimeException("Error retrieving next entry", e);
        }
    }

}