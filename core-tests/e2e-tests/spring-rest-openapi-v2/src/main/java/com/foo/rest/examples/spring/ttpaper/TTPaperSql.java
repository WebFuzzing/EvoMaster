package com.foo.rest.examples.spring.ttpaper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;

@RestController
public class TTPaperSql {

    @Autowired
    private EntityManager em;


    public void get(){
        //TODO
    }
}
