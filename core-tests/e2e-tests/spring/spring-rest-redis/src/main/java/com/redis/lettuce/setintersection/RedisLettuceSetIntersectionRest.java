package com.redis.lettuce.setintersection;

import com.redis.lettuce.AbstractRedisLettuceRest;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Set;

@RestController
@RequestMapping(path = "/redislettucesetintersection")
public class RedisLettuceSetIntersectionRest extends AbstractRedisLettuceRest {

    @PostMapping("/set/{key}/{member}")
    public ResponseEntity<Void> saveSetMember(@PathVariable String key, @PathVariable String member) {
        try{
            sync.sadd(key, member);
        } catch (Exception e) {
            return ResponseEntity.status(400).build();
        }
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/set/variable-intersection/{set1}/{set2}")
    public ResponseEntity<Void> getIntersection(@PathVariable String set1, @PathVariable String set2) {
        Set<String> result = sync.sinter(set1, set2);
        if (result != null && !result.isEmpty()) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

}


