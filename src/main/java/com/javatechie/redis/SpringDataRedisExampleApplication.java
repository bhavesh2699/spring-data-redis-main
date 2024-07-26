package com.javatechie.redis;

import java.util.Map;

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

    @GetMapping("/getNextEntry")
    public Map.Entry<String, String> getNextEntry(@RequestParam String key) {
        return roundRobinService.getNextEntry(key);
    }
    
    @PostMapping("/add")
    public ResponseEntity<Void> addValue(@RequestParam String key, @RequestParam String value) {
        roundRobinService.addValue(key, value);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
    
    public static void main(String[] args) {
        SpringApplication.run(SpringDataRedisExampleApplication.class, args);
    }

}
