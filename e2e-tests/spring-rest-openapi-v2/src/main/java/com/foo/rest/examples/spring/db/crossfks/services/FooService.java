package com.foo.rest.examples.spring.db.crossfks.services;

import com.foo.rest.examples.spring.db.crossfks.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@RestController
public class FooService {

    @Autowired
    private FooRepository fooRepository;

    @Autowired
    private RootRepository rootRepository;


    @RequestMapping(value = "/root/{rootName}/foo/{fooName}/bar", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public String getFooActivatedBars(@PathVariable("rootName") String rootName, @PathVariable("fooName") String fooName) {
        FooTableEntity nodeC = fooRepository.findNodeCTableEntitiesByRootTableEntityNameAndName(rootName, fooName);
        List<String> nodeBNames = new ArrayList<String>();
        for (BarTableEntity nodeB : nodeC.getActivatedBars()) {
            nodeBNames.add(nodeB.getName());
        }
        if (nodeBNames.isEmpty())
            return "EMPTY";
        return "NOT EMPTY";

    }

    @RequestMapping(value = "/root/{rootName}/foo/{fooName}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
    public Response createFoo(@PathVariable("rootName") String rootName, @PathVariable("fooName") String fooName) {
        RootTableEntity root = rootRepository.findByName(rootName);

        FooTableEntity entity = new FooTableEntity();
        entity.setName(fooName);
        entity.setRootTableDto(root);
        fooRepository.save(entity);
        return Response.status(201).build();
    }

    @RequestMapping(value = "/root/{rootName}/foo/{fooName}/bar/{barName}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON)
    public Response addBarFromFoo(@PathVariable("rootName") String rootName, @PathVariable("fooName") String fooName, @PathVariable("barName") String barName) {
        FooTableEntity foo = fooRepository.findNodeCTableEntitiesByRootTableEntityNameAndName(rootName, fooName);
        if (foo.hasActiveBars(barName))
            throw new RuntimeException("duplicated bar in foo");
        foo.active(barName);
        return Response.status(201).build();
    }
}
