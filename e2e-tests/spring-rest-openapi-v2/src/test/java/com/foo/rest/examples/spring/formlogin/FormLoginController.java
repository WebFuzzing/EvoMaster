package com.foo.rest.examples.spring.formlogin;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;

import java.util.Arrays;
import java.util.List;

public class FormLoginController extends SpringController {

    public FormLoginController(){
        super(FormLoginApplication.class);
    }


    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                AuthUtils.getForDefaultSpringFormLogin("user", "foo", "123456"),
                AuthUtils.getForDefaultSpringFormLogin("admin", "admin", "bar")
        );
    }
}
