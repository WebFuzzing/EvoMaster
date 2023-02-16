package com.foo.rest.examples.spring.db.crossfks.services;

import com.foo.rest.examples.spring.db.crossfks.entities.RootRepository;
import com.foo.rest.examples.spring.db.crossfks.entities.RootTableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
public class RootService {

    @Autowired
    private RootRepository rootRepository;


    @RequestMapping(value = "/root/{rootName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String getRoot(@PathParam("rootName") String rootName) {
        RootTableEntity found = rootRepository.findByName(rootName);
        if (found == null)
            return null;
        return found.getName();
    }

    @RequestMapping(value = "/root/{rootName}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
    public Response createFoo(@PathParam("rootName") String rootName) throws URISyntaxException {
        RootTableEntity root = new RootTableEntity();
        root.setName(rootName);
        rootRepository.save(root);
        return Response.created(new URI("/root/" + rootName)).build();
    }
}
