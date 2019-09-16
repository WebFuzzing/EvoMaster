package com.foo.rest.examples.spring.impact;

import javax.ws.rs.core.MediaType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * created by manzh on 2019-09-12
 */
@RestController
@RequestMapping(path = "/api")
public class ImpactRest {

    public static final List<NoImpactIntFieldsDto> data = new CopyOnWriteArrayList<>();

    @RequestMapping(
            value = "/intImpact",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON
    )
    public ResponseEntity create(@RequestBody NoImpactIntFieldsDto dto) {
        if (dto.name.isEmpty())
            return ResponseEntity.status(400).build();

        int distance = Integer.MAX_VALUE / 5;
        int response = 0;
        if (dto.impactIntField < (Integer.MIN_VALUE + distance * 2))
            response = 2;
        else if (dto.impactIntField < (Integer.MIN_VALUE + distance * 4))
            response = 4;
        else if (dto.impactIntField < (Integer.MIN_VALUE + distance * 6))
            response = 6;
        else if (dto.impactIntField < (Integer.MIN_VALUE + distance * 8))
            response = 8;
        else
            response = 10;

        data.add(dto);

        return ResponseEntity.status(201).build();
    }

    @RequestMapping(
            value = "/intImpact/{name}",
            method = RequestMethod.POST
    )
    public ResponseEntity create(
            @PathVariable("name") String name,
            @RequestParam("impactIntField") int impactIntField,
            @RequestParam("noimpactIntField") int noimpactIntField) {

        if (name.isEmpty())
            return ResponseEntity.status(400).build();

        int distance = Integer.MAX_VALUE / 5;
        int response = 0;
        if (impactIntField < (Integer.MIN_VALUE + distance * 2))
            response = 2;
        else if (impactIntField < (Integer.MIN_VALUE + distance * 4))
            response = 4;
        else if (impactIntField < (Integer.MIN_VALUE + distance * 6))
            response = 6;
        else if (impactIntField < (Integer.MIN_VALUE + distance * 8))
            response = 8;
        else
            response = 10;

        data.add(new NoImpactIntFieldsDto(name, impactIntField, noimpactIntField));

        return ResponseEntity.status(201).build();
    }
}
