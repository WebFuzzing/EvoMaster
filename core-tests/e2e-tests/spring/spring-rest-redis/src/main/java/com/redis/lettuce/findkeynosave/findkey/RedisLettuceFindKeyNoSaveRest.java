package com.redis.lettuce.findkeynosave.findkey;

import com.redis.lettuce.AbstractRedisLettuceRest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/redislettucefindkeynosave")
public class RedisLettuceFindKeyNoSaveRest extends AbstractRedisLettuceRest {

    @GetMapping("/findKey")
    public ResponseEntity<Void> findKey() {
        String result = sync.get("key-for-findkey");
        if (result != null) {
            return ResponseEntity.status(200).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

}


