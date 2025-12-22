package com.foo.rest.examples.spring.formlogin;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(path = "/api/formlogin", produces = MediaType.TEXT_PLAIN_VALUE)
public class FormLoginRest {

    @GetMapping(path = "/openToAll")
    public String openToAll() {
        return "openToAll";
    }

    @GetMapping(path = "/forUsers")
    public String forUsers() {
        return "forUsers";
    }

    @GetMapping(path = "/forAdmins")
    public String forAdmins() {
        return "forAdmins";
    }

}