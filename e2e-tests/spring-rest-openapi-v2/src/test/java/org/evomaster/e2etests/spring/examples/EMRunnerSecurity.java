package org.evomaster.e2etests.spring.examples;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.formlogin.FormLoginController;
import com.foo.rest.examples.spring.securitytest.SecurityTestController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;

public class EMRunnerSecurity {

    public static void main(String[] args){

        int port = 40100;
        if(args.length > 0){
            port = Integer.parseInt(args[0]);
        }

        SpringController controller = new SecurityTestController();
        controller.setControllerPort(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }
}