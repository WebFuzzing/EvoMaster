package com.foo.rest.examples.spring.db.directint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping(path = "/api/db/directint")
public class DbDirectIntRest {

    @Autowired
    private DbDirectIntRepository repository;


    @RequestMapping(
            method = RequestMethod.POST
    )
    public void post() {
        DbDirectIntEntity entity = new DbDirectIntEntity();
        entity.setX(42);
        entity.setY(77);
        repository.save(entity);
    }


    @RequestMapping(
            path = "/{x}/{y}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity get(@PathVariable("x") int x, @PathVariable("y") int y) {

        List<DbDirectIntEntity> list = repository.findByXIsAndYIs(x, y);
        if (list.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }
}
