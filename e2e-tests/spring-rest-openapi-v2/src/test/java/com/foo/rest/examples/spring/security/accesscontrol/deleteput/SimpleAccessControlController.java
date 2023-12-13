package com.foo.rest.examples.spring.security.accesscontrol.deleteput;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.db.SpringWithDbController;
import com.foo.rest.examples.spring.db.directint.DbDirectIntApplication;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;

import java.util.Arrays;
import java.util.List;

public class SimpleAccessControlController extends SpringController {

    public SimpleAccessControlController() {
        super(SimpleAccessControlApplication.class);
    }


    public static void main(String[] args){
        com.foo.rest.examples.spring.security.accesscontrol.deleteput.SimpleAccessControlController controller = new com.foo.rest.examples.spring.security.accesscontrol.deleteput.SimpleAccessControlController();
        controller.setControllerPort(40100);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);
        starter.start();
    }

    @Override
    public void resetStateOfSUT(){
        SimpleAccessControlRest.resetState();
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                AuthUtils.getForBasic("creator", "creator0", "creator_password"),
                AuthUtils.getForBasic("creator", "creator1", "creator_password")
//                AuthUtils.getForBasic("consumer1", "consumer1", "consumer1_password"),
//                AuthUtils.getForBasic("consumer2", "consumer2", "consumer2_password")
        );
    }
}
