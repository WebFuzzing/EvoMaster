package org.evomaster.e2etests.spring.examples;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.headerlocation.HeaderLocationController;
import org.evomaster.clientJava.controller.InstrumentedSutStarter;

/**
 * Only used for manual debugging
 */
public class EMRunner {

    public static void main(String[] args){

        int port = 40100;
        if(args.length > 0){
            port = Integer.parseInt(args[0]);
        }

        SpringController controller = new HeaderLocationController();
        controller.setControllerPort(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }
}
