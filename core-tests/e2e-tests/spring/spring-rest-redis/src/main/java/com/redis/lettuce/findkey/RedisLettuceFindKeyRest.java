package com.redis.lettuce.findkey;

import com.redis.lettuce.AbstractRedisLettuceRest;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.RedisClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(path = "/redislettucefindkey")
public class RedisLettuceFindKeyRest extends AbstractRedisLettuceRest {

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


