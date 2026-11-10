package com.example.redis.lettuce.richentity;

import com.redis.lettuce.AbstractRedisLettuceRest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping(path = "/richentity")
public class RichEntityRest extends AbstractRedisLettuceRest {

    private final RichEntityRepository repository;

    public RichEntityRest(RichEntityRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<Void> findById(@PathVariable("id") String id) {
        Optional<RichEntity> found = repository.findById(id);
        if (!found.isPresent()) {
            return ResponseEntity.status(404).build();
        }
        RichEntity entity = found.get();

        // only an entity that is the one asked for, and whose nested object was populated
        if (id.equals(entity.getId())
                && entity.getAddress() != null
                && entity.getAddress().getCity() != null) {
            return ResponseEntity.status(200).build();
        }
        if (entity.getTags() != null && !entity.getTags().isEmpty()) {
            return ResponseEntity.status(203).build();
        }
        return ResponseEntity.status(201).build();
    }

}
