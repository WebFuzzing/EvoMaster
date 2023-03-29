package com.foo.rest.examples.spring.db.crossfks.services;

import com.foo.rest.examples.spring.db.crossfks.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(path = "/api/root")
public class BarService {

    @Autowired
    private BarRepository barRepository;

    @Autowired
    private RootRepository rootRepository;

    @RequestMapping(value = "/{rootName}/bar/{barName}", method = RequestMethod.GET, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String getRoot(@PathVariable("rootName") String rootName, @PathVariable("barName") String barName) {
        BarTableEntity found = barRepository.findBarTableEntityByRootTableEntityNameAndName(rootName, barName);
        if (found == null)
            return null;
        return found.getName();
    }

    @RequestMapping(value = "/{rootName}/bar/{barName}", method = RequestMethod.POST)
    public ResponseEntity createBar(@PathVariable("rootName") String rootName, @PathVariable("barName") String barName) {
        RootTableEntity root = rootRepository.findByName(rootName);

        if (root.hasBarNamed(barName))
            throw new RuntimeException("duplicated bar");

        BarTableEntity bar = BarTableEntity.withName(root, barName);
        barRepository.save(bar);
        return ResponseEntity.status(201).build();
    }
}
