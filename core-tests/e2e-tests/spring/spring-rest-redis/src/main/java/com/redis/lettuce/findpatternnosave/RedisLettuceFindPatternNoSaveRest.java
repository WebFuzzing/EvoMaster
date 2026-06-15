package com.redis.lettuce.findpatternnosave;

import com.redis.lettuce.AbstractRedisLettuceRest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/redislettucefindpatternnosave")
public class RedisLettuceFindPatternNoSaveRest extends AbstractRedisLettuceRest {

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


