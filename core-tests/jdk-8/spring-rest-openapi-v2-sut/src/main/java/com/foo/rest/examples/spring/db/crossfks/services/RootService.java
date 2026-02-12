package com.foo.rest.examples.spring.db.crossfks.services;

import com.foo.rest.examples.spring.db.crossfks.entities.RootRepository;
import com.foo.rest.examples.spring.db.crossfks.entities.RootTableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping(path = "/api/root")
public class RootService {

    @Autowired
    private RootRepository rootRepository;


    @RequestMapping(value = "/{rootName}", method = RequestMethod.GET)
    public String getRoot(@PathVariable("rootName") String rootName) {
        RootTableEntity found = rootRepository.findByName(rootName);
        if (found == null)
            return null;
        return found.getName();
    }

    @RequestMapping(value = "/{rootName}", method = RequestMethod.POST)
    public ResponseEntity createFoo(@PathVariable("rootName") String rootName) {
        RootTableEntity found = rootRepository.findByName(rootName);
        if (found != null)
            return  ResponseEntity.status(400).build();

        RootTableEntity root = new RootTableEntity();
        root.setName(rootName);
        rootRepository.save(root);
        return ResponseEntity.status(201).build();
    }
}
