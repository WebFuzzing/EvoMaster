package com.foo.rest.examples.spring.db.crossfks.services;

import com.foo.rest.examples.spring.db.crossfks.entities.RootRepository;
import com.foo.rest.examples.spring.db.crossfks.entities.RootTableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@RestController
@RequestMapping(path = "/api/root")
public class RootService {

    @Autowired
    private RootRepository rootRepository;


    @RequestMapping(value = "/{rootName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String getRoot(@PathVariable("rootName") String rootName) {
        RootTableEntity found = rootRepository.findByName(rootName);
        if (found == null)
            return null;
        return found.getName();
    }

    @RequestMapping(value = "/{rootName}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
    public Response createFoo(@PathVariable("rootName") String rootName) {
        RootTableEntity root = new RootTableEntity();
        root.setName(rootName);
        rootRepository.save(root);
        return Response.status(201).build();
    }
}
