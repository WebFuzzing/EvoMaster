package com.foo.rest.examples.spring.branches;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.positiveinteger.PIApplication;
import org.evomaster.clientJava.controller.RestController;
import org.evomaster.clientJava.controllerApi.dto.AuthenticationDto;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
import java.util.Map;

public class BranchesController extends SpringController {

    public BranchesController() {
        super(BranchesApplication.class);
    }

}
