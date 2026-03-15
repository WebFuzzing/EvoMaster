package com.redis.lettuce.setmembers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Set;

@RestController
@RequestMapping(path = "/redislettucesetmembers")
public class RedisLettuceSetMembersRest {

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

    @PostMapping("/set/{key}/{member}")
    public ResponseEntity<Void> saveSetMember(@PathVariable String key, @PathVariable String member) {
        try{
            sync.sadd(key, member);
        } catch (Exception e) {
            return ResponseEntity.status(400).build();
        }
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/set/members/{key}")
    public ResponseEntity<Void> getMembers(@PathVariable String key) {
        Set<String> result = sync.smembers(key);
        if (result != null && !result.isEmpty()) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

}


