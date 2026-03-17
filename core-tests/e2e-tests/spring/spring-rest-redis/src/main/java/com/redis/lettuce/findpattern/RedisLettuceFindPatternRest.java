package com.redis.lettuce.findpattern;

import com.redis.lettuce.AbstractRedisLettuceRest;
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
@RequestMapping(path = "/redislettucefindpattern")
public class RedisLettuceFindPatternRest extends AbstractRedisLettuceRest {

    @PostMapping("/string/{key}")
    public ResponseEntity<Void> saveData(@PathVariable String key) {
        sync.set(key, "value");
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/findPattern")
    public ResponseEntity<Void> findPattern() {

        List<String> keys = sync.keys("som[3e]-key-?-*");
        if (keys != null && !keys.isEmpty()) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

}


