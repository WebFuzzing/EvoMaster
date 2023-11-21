package com.foo.rest.examples.spring.securitytest;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;

import java.util.Arrays;
import java.util.List;

public class SecurityTestController extends SpringController {

    public SecurityTestController(){
        super(SecurityTestApplication.class);
    }




    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {

        return Arrays.asList(
                AuthUtils.getForDefaultSpringFormLogin("UserA", "UserA", "userA"),
                AuthUtils.getForDefaultSpringFormLogin("UserB", "UserB", "userB")
        );
    }



}
