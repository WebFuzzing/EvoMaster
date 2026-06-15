package com.redis.lettuce.setmembersnosave;

import com.redis.lettuce.AbstractRedisLettuceRest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping(path = "/redislettucesetmembersnosave")
public class RedisLettuceSetMembersNoSaveRest extends AbstractRedisLettuceRest {

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


