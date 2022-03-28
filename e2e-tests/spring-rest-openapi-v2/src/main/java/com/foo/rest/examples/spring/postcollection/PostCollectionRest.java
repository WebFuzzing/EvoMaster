package com.foo.rest.examples.spring.postcollection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping(path = "/api/pc")
public class PostCollectionRest {

    public static final List<Integer> data = new CopyOnWriteArrayList<>();

    @RequestMapping(
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON
    )
    public ResponseEntity create(@RequestBody CreateDto dto) {

        if (dto.value != null) {
            data.add(dto.value);
        }

        return ResponseEntity.status(201).build();
    }


    @RequestMapping(
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity<ValuesDto> get() {

        if (data.size() < 7) {
            return ResponseEntity.status(400).build();
        }

        ValuesDto dto = new ValuesDto();
        dto.values.addAll(data);

        return ResponseEntity.ok(dto);
    }
}
