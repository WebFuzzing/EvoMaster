package com.redis.lettuce.setintersectionnosave;

import com.redis.lettuce.AbstractRedisLettuceRest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping(path = "/redislettucesetintersectionnosave")
public class RedisLettuceSetIntersectionNoSaveRest extends AbstractRedisLettuceRest {

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


