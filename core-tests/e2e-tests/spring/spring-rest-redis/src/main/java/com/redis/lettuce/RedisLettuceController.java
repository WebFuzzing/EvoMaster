package com.redis.lettuce;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.RedisClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@RestController
@RequestMapping(path = "/redislettuce")
public class RedisLettuceController {

    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String, String> sync;

    @PostConstruct
    public void init() {
        String redisHost = System.getProperty("spring.redis.host", "localhost");
        String redisPort = System.getProperty("spring.redis.port", "6379");
        String redisUri = "redis://" + redisHost + ":" + redisPort;
        redisClient = RedisClient.create(redisUri);
        connection = redisClient.connect();
        sync = connection.sync();
    }

    @PreDestroy
    public void shutdown() {
        connection.close();
        redisClient.shutdown();
    }

    @PostMapping("/string/{key}")
    public ResponseEntity<Void> saveData(@PathVariable String key) {
        sync.set(key, "value");
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/findKey/{key}")
    public ResponseEntity<Void> findKey(@PathVariable String key) {
        String result = sync.get(key);
        if (result != null) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }
}


