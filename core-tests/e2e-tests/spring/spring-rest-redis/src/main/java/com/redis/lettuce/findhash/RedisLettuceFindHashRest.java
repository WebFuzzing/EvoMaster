package com.redis.lettuce.findhash;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(path = "/redislettucefindhash")
public class RedisLettuceFindHashRest {

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

    @PostMapping("/hash/{key}/{field}")
    public ResponseEntity<Void> saveHash(@PathVariable String key, @PathVariable String field) {
        sync.hset(key, field, "value");
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/findHash/{key}")
    public ResponseEntity<Void> findHash(@PathVariable String key) {
        String result = sync.hget(key, "field");
        if (result != null) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

}


