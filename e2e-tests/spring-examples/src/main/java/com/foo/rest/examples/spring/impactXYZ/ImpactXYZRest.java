package com.foo.rest.examples.spring.impactXYZ;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping(path = "/api/impactxyz")
public class ImpactXYZRest {

    public static final List<XYZDto> data = new CopyOnWriteArrayList<>();


    @RequestMapping(
            value = "/{x}",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String create(
            @PathVariable("x") int x,
            @RequestParam("y") String y,
            @RequestParam("z") String z) {

        if (x < 1000)
            throw new IllegalArgumentException("invalid inputs");
        if (!y.equals("foo"))
            return "NOT_MATCHED";

        if (data.size() == 4)
            return "EXCEED";

        int response = 0;
        if (x < 10000)
            response = 1;
        else if (x < 20000)
            response = 2;
        else if (x < 30000)
            response = 3;
        else {
            response = 4;
        }
        data.add(new XYZDto(x, y, z));

        return "CREATED_"+response;
    }
}
