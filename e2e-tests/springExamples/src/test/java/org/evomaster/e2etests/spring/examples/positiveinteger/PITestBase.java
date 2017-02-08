package org.evomaster.e2etests.spring.examples.positiveinteger;

import com.foo.rest.examples.spring.positiveinteger.PIController;
import org.evomaster.clientJava.controller.EmbeddedStarter;
import org.evomaster.clientJava.controllerApi.SutInfoDto;
import org.evomaster.core.problem.rest.service.RemoteController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class PITestBase extends SpringTestBase {


    @BeforeAll
    public static void initClass() throws Exception {

        PIController controller = new PIController();
        SpringTestBase.initClass(controller);
    }
}
