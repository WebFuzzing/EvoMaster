package com.foo.rest.examples.spring.testability;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.taintMulti.TaintMultiApplication;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TestabilityController extends SpringController {

    public TestabilityController(){
        super(TestabilityApplication.class);
    }

    public TestabilityController(int port){
        super(TestabilityApplication.class);
        setControllerPort(port);
    }

    public static void main(String[] args) {

        TestabilityController controller = new TestabilityController(40100);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }
}
