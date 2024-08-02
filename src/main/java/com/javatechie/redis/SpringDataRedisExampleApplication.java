package com.javatechie.redis;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
	
	String queueNames = "q1,q2,q3,q4,q5"; //for testing, will need to pass values from DB

    @GetMapping("/getNextEntry")
    public String getNextEntry(@RequestParam String disseminationProfileId) {
    	Map<Object, Object> cachedQueues = roundRobinService.initializeCache(disseminationProfileId, queueNames);
    	if(cachedQueues != null) 
    		return roundRobinService.getNextEntry(disseminationProfileId, cachedQueues);
    	else {
            System.out.println("Error: Cache is NULL/Empty "+cachedQueues);
    		return null;
    	}
    }
    
    /*@PostMapping("/add")
    public ResponseEntity<Void> addValue(@RequestParam String key, @RequestParam String value) {
        roundRobinService.addValue(key, value);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }*/
    
    public static void main(String[] args) {
        SpringApplication.run(SpringDataRedisExampleApplication.class, args);
    }

}
