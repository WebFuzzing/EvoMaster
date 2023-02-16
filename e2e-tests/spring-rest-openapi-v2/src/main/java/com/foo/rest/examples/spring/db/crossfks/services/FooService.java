package com.foo.rest.examples.spring.db.crossfks.services;

import com.foo.rest.examples.spring.db.crossfks.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class FooService {

    @Autowired
    private FooRepository fooRepository;

    @Autowired
    private RootRepository rootRepository;


    @RequestMapping(value = "/root/{rootName}/foo/{fooName}/bar", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public List<String> getFooActivedBars(@PathParam("rootName") String rootName, @PathParam("fooName") String fooName) {
        FooTableEntity nodeC = fooRepository.findNodeCTableEntitiesByRootTableEntityNameAndName(rootName, fooName);
        List<String> nodeBNames = new ArrayList<String>();
        for (BarTableEntity nodeB : nodeC.getActivedBars()) {
            nodeBNames.add(nodeB.getName());
        }
        return nodeBNames;

    }

    @RequestMapping(value = "/root/{rootName}/foo/{fooName}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
    public Response createFoo(@PathParam("rootName") String rootName, @PathParam("fooName") String fooName) throws URISyntaxException {
        RootTableEntity root = rootRepository.findByName(rootName);

        FooTableEntity entity = new FooTableEntity();
        entity.setName(fooName);
        entity.setRootTableDto(root);
        fooRepository.save(entity);
        return Response.created(new URI("/root/" + rootName + "/foo/" + fooName)).build();
    }

    @RequestMapping(value = "/root/{rootName}/foo/{fooName}/bar/{barName}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
    public Response addBarFromFoo(@PathParam("rootName") String rootName, @PathParam("fooName") String fooName, @PathParam("barName") String barName) throws URISyntaxException {
        FooTableEntity foo = fooRepository.findNodeCTableEntitiesByRootTableEntityNameAndName(rootName, fooName);
        if (foo.hasActiveBars(barName))
            throw new RuntimeException("duplicated bar in foo");
        foo.active(barName);
        return Response.created(new URI("/root/" + rootName + "/foo/" + fooName+ "/bar/" + barName)).build();
    }
}
