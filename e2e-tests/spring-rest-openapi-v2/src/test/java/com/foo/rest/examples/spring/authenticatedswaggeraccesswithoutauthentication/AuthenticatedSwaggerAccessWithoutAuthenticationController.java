package com.foo.rest.examples.spring.authenticatedswaggeraccesswithoutauthentication;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;

public class AuthenticatedSwaggerAccessWithoutAuthenticationController extends SpringController {

    public AuthenticatedSwaggerAccessWithoutAuthenticationController() {
        super(AuthenticatedSwaggerAccessWithoutAuthenticationApplication.class);
    }


    public static void main(String[] args){
        AuthenticatedSwaggerAccessWithoutAuthenticationController controller = new AuthenticatedSwaggerAccessWithoutAuthenticationController();
        controller.setControllerPort(40100);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);
        starter.start();
    }

}
