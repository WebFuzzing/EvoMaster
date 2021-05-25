package com.foo.rest.examples.spring.db.auth;


import com.foo.rest.examples.spring.db.auth.db.AuthUserEntity;
import com.foo.rest.examples.spring.db.auth.db.AuthUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/db/auth")
public class AuthRest {

    @Autowired
    private AuthProjectService projectService;

    @Autowired
    private AuthUserRepository userRepository;

    @GetMapping(path = "/projects")
    public ResponseEntity getProjects(Authentication user){

        String id = user.getName();

        int n = projectService.getForUser(id).size();

        if(n == 0){
            return ResponseEntity.status(400).build();
        }

        return ResponseEntity.status(200).build();
    }


    @GetMapping(path = "/users")
    public Iterable<AuthUserEntity> getUsers(){

        return userRepository.findAll();
    }
}
