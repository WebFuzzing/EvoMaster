package com.foo.rest.examples.spring.impactXYZ;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping(path = "/api/impactxyz")
public class ImpactXYZRest {

    public static final List<XYZDto> data = new CopyOnWriteArrayList<>();


    @RequestMapping(
            value = "/{x}",
            method = RequestMethod.POST,
            produces = MediaType.TEXT_PLAIN
    )
    public ResponseEntity create(
            @PathVariable("x") int x,
            @RequestParam("y") String y,
            @RequestParam("z") String z) {

        if (x < 1000)
            return ResponseEntity.status(400).build();
        if (!y.equals("foo"))
            return ResponseEntity.status(400).build();

        int response = 0;
        if (data.size() > 100)
            return ResponseEntity.ok(response);

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

        return ResponseEntity.ok(response);
    }
}
