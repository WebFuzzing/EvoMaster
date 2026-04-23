package com.foo.rest.examples.spring.db.preparedstatement;

import com.foo.rest.examples.spring.db.SpringWithDbController;
import com.foo.rest.examples.spring.db.directint.DbDirectIntApplication;
import com.foo.rest.examples.spring.db.directint.DbDirectIntController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;

public class PreparedStatementController extends SpringWithDbController {

    public PreparedStatementController() {
        super(PreparedStatementApplication.class);
    }

    public static void main(String[] args){
        PreparedStatementController controller = new PreparedStatementController();
        controller.setControllerPort(40100);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);
        starter.start();
    }
}
