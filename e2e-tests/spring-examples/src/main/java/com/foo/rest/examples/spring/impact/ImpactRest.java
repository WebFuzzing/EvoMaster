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
@RequestMapping(path = "/api/intImpact")
public class ImpactRest {

    public static final List<NoImpactIntFieldsDto> data = new CopyOnWriteArrayList<>();

    @RequestMapping(
            value = "",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON
    )
    public ResponseEntity create(@RequestBody NoImpactIntFieldsDto dto) {
        if (dto.name.isEmpty())
            return ResponseEntity.status(400).build();

        int response = 0;
        if (dto.impactIntField >= 0 && dto.impactIntField < 5)
            response = 5;
        else if (dto.impactIntField >= 5 && dto.impactIntField < 10)
            response = 10;
        else if (dto.impactIntField >= 10 && dto.impactIntField < 15)
            response = 15;
        else response = 20;

        data.add(dto);

        return ResponseEntity.status(201).build();
    }

    @RequestMapping(
            value = "/{name}",
            method = RequestMethod.POST
    )
    public ResponseEntity create(
            @PathVariable("name") String name,
            @RequestParam("impactIntField") int impactIntField,
            @RequestParam("noimpactIntField") int noimpactIntField) {

        if (name.isEmpty())
            return ResponseEntity.status(400).build();

        int response = 0;
        if (impactIntField >= 0 && impactIntField < 5)
            response = 5;
        else if (impactIntField >= 5 && impactIntField < 10)
            response = 10;
        else if (impactIntField >= 10 && impactIntField < 15)
            response = 15;
        else response = 20;

        data.add(new NoImpactIntFieldsDto(name, impactIntField, noimpactIntField));

        return ResponseEntity.status(201).build();
    }
}
