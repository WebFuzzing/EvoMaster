package com.foo.rest.examples.spring.security.accesscontrol.deleteput;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;

import java.util.Arrays;
import java.util.List;

public class ACDeletePutController extends SpringController {

    public ACDeletePutController() {
        super(ACDeletePutApplication.class);
    }


    public static void main(String[] args){
        ACDeletePutController controller = new ACDeletePutController();
        controller.setControllerPort(40100);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);
        starter.start();
    }

    @Override
    public void resetStateOfSUT(){
        ACDeletePutRest.resetState();
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                AuthUtils.getForBasic("creator0", "creator0", "creator_password"),
                AuthUtils.getForBasic("creator1", "creator1", "creator_password")
        );
    }
}
