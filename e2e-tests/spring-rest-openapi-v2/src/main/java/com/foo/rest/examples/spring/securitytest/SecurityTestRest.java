package com.foo.rest.examples.spring.securitytest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/securityTest", produces = MediaType.TEXT_PLAIN_VALUE)
public class SecurityTestRest {

    private String userAInformation;
    private String userBInformation;

    private String userAPrivateInformation;
    private String userBPrivateInformation;

    @GetMapping(path = "/publicInformation")
    public String openToAll() {
        return "publicInformation";
    }

    @GetMapping(path = "/userA")
    public String readUserAInformation() {

        return this.userAInformation;
    }

    @PostMapping(path = "/userA")
    public void createUserAInformation() {

        this.userAInformation = "userAInfo";
    }

    @DeleteMapping(path = "/userA")
    public void deleteUserAInformation() {

        this.userAInformation = null;
    }


    @GetMapping(path = "/userAPrivate")
    public String readUserAPrivateInformation() {

        return this.userAPrivateInformation;
    }

    @PostMapping(path = "/userAPrivate")
    public void createUserAPrivateInformation() {

        this.userAPrivateInformation = "userAPrivate";
    }

    @DeleteMapping(path = "/userAPrivate")
    public void deleteUserAPrivateInformation() {

        this.userAPrivateInformation = null;
    }

    @GetMapping(path = "/userB")
    public String readUserBInformation() {

        return this.userBInformation;
    }

    @PostMapping(path = "/userB")
    public void createUserBInformation() {

        this.userBInformation = "userBInfo";
    }

    @DeleteMapping(path = "/userB")
    public void deleteUserBInformation() {

        this.userBInformation = null;
    }

    @GetMapping(path = "/userBPrivate")
    public String readUserBPrivateInformation() {

        return this.userBPrivateInformation;
    }

    @PostMapping(path = "/userBPrivate")
    public void createUserBPrivateInformation() {

        this.userBPrivateInformation = "userBPrivate";
    }

    @DeleteMapping(path = "/userBPrivate")
    public void deleteUserBPrivateInformation() {

        this.userBPrivateInformation = null;
    }


}