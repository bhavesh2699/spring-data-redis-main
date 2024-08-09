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

@Service
public class RoundRobinService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private final Map<String, RedisAtomicInteger> counterCache = new ConcurrentHashMap<>();

    public List<String> initializeCache(String disseminationProfileId, String queueNames) {
        ListOperations<String, String> listOps = redisTemplate.opsForList();
        String[] splitedQueues = queueNames.split(",");
        List<String> listOfQueues = new ArrayList<>(Arrays.asList(splitedQueues));
        
        try {
            listOps.rightPushAll("ISO_CACHE_DISSEMINATION_ROUNDROBIN::ISO_CACHE_DISSEMINATION_ROUNDROBIN_" + disseminationProfileId, listOfQueues);
        } catch (DataAccessException e) {
            // Handle Redis-specific exceptions
            System.err.println("DataAccessException occurred while initializing cache: " + e.getMessage());
            throw new RuntimeException("Error initializing cache due to data access issue", e);
        } catch (IndexOutOfBoundsException e) {
            // Handle issues related to list operations
            System.err.println("IndexOutOfBoundsException occurred while initializing cache: " + e.getMessage());
            throw new RuntimeException("Error initializing cache due to index issue", e);
        } catch (Exception e) {
            // Handle all other exceptions
            System.err.println("Unexpected error occurred while initializing cache: " + e.getMessage());
            throw new RuntimeException("Unexpected error occurred while initializing cache", e);
        }
        return listOfQueues;
    }

    public synchronized String getNextEntry(String disseminationProfileId, List<String> cachedQueues) {
        if (cachedQueues == null || cachedQueues.isEmpty()) {
            throw new IllegalArgumentException("Cached queues must not be null or empty");
        }
        
        String counterKey = "ISO_CACHE_DISSEMINATION_ROUNDROBIN::ISO_CACHE_DISSEMINATION_ROUNDROBIN_LAST_ACCESSED_IDX_" + disseminationProfileId;
        if (redisTemplate.hasKey(counterKey) == Boolean.FALSE) {
            counterCache.remove(counterKey);
        }
        RedisAtomicInteger counter = counterCache.computeIfAbsent(counterKey, k -> new RedisAtomicInteger(k, redisConnectionFactory));

        try {
            int index = counter.getAndIncrement() % cachedQueues.size();
            if (counter.get() >= (Integer.MAX_VALUE / 2)) {
                counter.compareAndSet(counter.get(), 0);
            }
            return cachedQueues.get(index);
        } catch (DataAccessException e) {
            // Handle Redis-specific exceptions
            System.err.println("DataAccessException occurred while getting next entry: " + e.getMessage());
            throw new RuntimeException("Error getting next entry due to data access issue", e);
        } catch (IndexOutOfBoundsException e) {
            // Handle issues related to index access
            System.err.println("IndexOutOfBoundsException occurred while getting next entry: " + e.getMessage());
            throw new RuntimeException("Error getting next entry due to index issue", e);
        } catch (ConcurrentModificationException e) {
            // Handle issues related to concurrent modifications
            System.err.println("ConcurrentModificationException occurred while getting next entry: " + e.getMessage());
            throw new RuntimeException("Error getting next entry due to concurrent modification", e);
        } catch (Exception e) {
            // Handle all other exceptions
            System.err.println("Unexpected error occurred while getting next entry: " + e.getMessage());
            throw new RuntimeException("Unexpected error occurred while getting next entry", e);
        }
    }
}
