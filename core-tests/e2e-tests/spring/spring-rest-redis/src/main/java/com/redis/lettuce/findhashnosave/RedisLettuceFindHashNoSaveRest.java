package com.redis.lettuce.findhashnosave;

import com.redis.lettuce.AbstractRedisLettuceRest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/redislettucefindhashnosave")
public class RedisLettuceFindHashNoSaveRest extends AbstractRedisLettuceRest {

    @GetMapping("/findHash")
    public ResponseEntity<Void> findHashNoSave() {
        String result = sync.hget("key-for-findhash", "field");
        if (result != null) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/findHashAllFields")
    public ResponseEntity<Void> findHashesNoSave() {
        var result = sync.hgetall("another-key-for-findhashes");
        if (result != null && !result.isEmpty()) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

}


