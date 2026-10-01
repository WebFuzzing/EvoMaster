package com.example.redis.lettuce.entity;

import com.redis.lettuce.AbstractRedisLettuceRest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(path = "/entity")
public class EntityRest extends AbstractRedisLettuceRest {

    private final EntityRepository entityRepository;

    public EntityRest(EntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<Void> findById(@PathVariable("id") String id) {
        Optional<Entity> optionalEntity = entityRepository.findById(id);
        if (optionalEntity.isPresent()) {
            Entity entity = optionalEntity.get();
            if (entity.getAddress()!=null) {
                return ResponseEntity.status(200).build();
            }
            return ResponseEntity.status(201).build();
        } else {
            return ResponseEntity.status(404).build();
        }
    }

}


