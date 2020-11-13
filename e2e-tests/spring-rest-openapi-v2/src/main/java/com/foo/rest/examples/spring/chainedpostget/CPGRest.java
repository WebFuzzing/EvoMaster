package com.foo.rest.examples.spring.chainedpostget;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping(path = "/api/cpg")
public class CPGRest {

    public static final Map<Integer, CPGRest.X> data = new ConcurrentHashMap<>();

    private final AtomicInteger counter = new AtomicInteger(0);


    @RequestMapping(
            path = "/x",
            method = RequestMethod.POST
    )
    public ResponseEntity createX(){

        int index = counter.incrementAndGet();
        X x = new X();
        data.put(index, x);

        return ResponseEntity.created(URI.create("/api/cpg/x/"+index)).build();
    }


    @RequestMapping(
            path = "/x/{id}/y",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity createY(@PathVariable("id") int idx,
                                  @RequestBody Y body){

        X x= data.get(idx);
        if(x == null){
            return ResponseEntity.status(404).build();
        }

        x.y = body;

        return ResponseEntity.status(201).build();
    }

    @RequestMapping(
            path = "/x/{id}/y",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public ResponseEntity<Y> getY(@PathVariable("id") int idx){

        X x= data.get(idx);
        if(x == null || x.y == null){
            return ResponseEntity.status(404).build();
        }

        return ResponseEntity.ok(x.y);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity handleExceptions(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(500).build();
    }


    private class X{
        public Y y;
    }
}
