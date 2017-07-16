package com.foo.rest.examples.spring.chainednolocation;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping(path = "/api")
public class CNLRest {

    public static final Map<Integer, CNL_X> data = new ConcurrentHashMap<>();

    private final AtomicInteger counter = new AtomicInteger(1234);


    @PostMapping(
            path = "/x",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CNL_X> createX(@RequestBody CNL_X input) {

        if (input == null || input.id != null) {
            return ResponseEntity.status(400).build();
        }

        int id = counter.incrementAndGet();
        input.id = id;
        data.put(id, input);

        return ResponseEntity.status(201).body(input);
    }


    @GetMapping(
            path = "/x/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CNL_X> getX(@PathVariable("id") int id){

        if(! data.containsKey(id)){
            return ResponseEntity.status(404).build();
        }

        return ResponseEntity.ok(data.get(id));
    }
}
