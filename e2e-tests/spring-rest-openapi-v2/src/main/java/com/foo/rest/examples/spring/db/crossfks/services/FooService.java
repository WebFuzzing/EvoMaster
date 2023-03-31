package com.foo.rest.examples.spring.db.crossfks.services;

import com.foo.rest.examples.spring.db.crossfks.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "/api/root")
public class FooService {

    @Autowired
    private FooRepository fooRepository;

    @Autowired
    private RootRepository rootRepository;


    @RequestMapping(value = "/{rootName}/foo/{fooName}/bar", method = RequestMethod.GET)
    public String getFooActivatedBars(@PathVariable("rootName") String rootName, @PathVariable("fooName") String fooName) {
        FooTableEntity foo = fooRepository.findFooTableEntitiesByRootTableEntityNameAndName(rootName, fooName);
        List<String> nodeBNames = new ArrayList<String>();
        for (BarTableEntity nodeB : foo.getActivatedBars()) {
            nodeBNames.add(nodeB.getName());
        }
        if (nodeBNames.isEmpty())
            return "EMPTY";
        return "NOT EMPTY";

    }

    @RequestMapping(value = "/{rootName}/foo/{fooName}", method = RequestMethod.POST)
    public ResponseEntity createFoo(@PathVariable("rootName") String rootName, @PathVariable("fooName") String fooName) {
        RootTableEntity root = rootRepository.findByName(rootName);

        FooTableEntity entity = new FooTableEntity();
        entity.setName(fooName);
        entity.setRootTableDto(root);
        fooRepository.save(entity);
        return ResponseEntity.status(201).build();
    }

    @RequestMapping(value = "/{rootName}/foo/{fooName}/bar/{barName}", method = RequestMethod.POST)
    public ResponseEntity addBarFromFoo(@PathVariable("rootName") String rootName, @PathVariable("fooName") String fooName, @PathVariable("barName") String barName) {
        FooTableEntity foo = fooRepository.findFooTableEntitiesByRootTableEntityNameAndName(rootName, fooName);
        if (foo.hasActiveBars(barName))
            throw new RuntimeException("duplicated bar in foo");
        foo.active(barName);
        fooRepository.save(foo);
        return ResponseEntity.status(201).build();
    }
}
