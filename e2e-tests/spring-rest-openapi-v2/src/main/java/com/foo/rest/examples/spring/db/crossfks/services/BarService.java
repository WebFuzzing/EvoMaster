package com.foo.rest.examples.spring.db.crossfks.services;

import com.foo.rest.examples.spring.db.crossfks.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RestController
public class BarService {

    @Autowired
    private BarRepository barRepository;

    @Autowired
    private RootRepository rootRepository;

    @RequestMapping(value = "/root/{rootName}/bar/{barName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String getRoot(@PathVariable("rootName") String rootName, @PathVariable("barName") String barName) {
        BarTableEntity found = barRepository.findBarTableEntityByRootTableEntityNameAndName(rootName, barName);
        if (found == null)
            return null;
        return found.getName();
    }

    @RequestMapping(value = "/root/{rootName}/bar/{barName}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
    public Response createFoo(@PathVariable("rootName") String rootName, @PathVariable("barName") String barName) {
        RootTableEntity root = rootRepository.findByName(rootName);

        if (root.hasBarNamed(barName))
            throw new RuntimeException("duplicated bar");

        BarTableEntity bar = BarTableEntity.withName(root, barName);
        barRepository.save(bar);
        return Response.status(201).build();
    }
}
