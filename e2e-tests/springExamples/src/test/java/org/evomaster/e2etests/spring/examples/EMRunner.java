package org.evomaster.e2etests.spring.examples;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.headerlocation.HeaderLocationController;
import com.foo.rest.examples.spring.triangle.TriangleController;
import org.evomaster.clientJava.controller.EmbeddedStarter;

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
        EmbeddedStarter starter = new EmbeddedStarter(controller);

        starter.start();
    }
}
