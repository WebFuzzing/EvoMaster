package org.evomaster.e2etests.spring.examples.positiveinteger;

import com.foo.rest.examples.spring.positiveinteger.PIController;
import org.evomaster.clientJava.controller.EmbeddedStarter;
import org.evomaster.clientJava.controllerApi.SutInfoDto;
import org.evomaster.core.problem.rest.service.RemoteController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class PITestBase {

    protected static EmbeddedStarter embeddedStarter;
    protected static String baseUrlOfSut;
    protected static RemoteController remoteController;
    protected static int controllerPort;

    @BeforeAll
    public static void initClass() throws Exception {

        PIController controller = new PIController();

        embeddedStarter = new EmbeddedStarter(controller);
        embeddedStarter.start();

        controllerPort = embeddedStarter.getControllerServerJettyPort();

        remoteController = new RemoteController("localhost", controllerPort);
        boolean started = remoteController.startSUT();
        assertTrue(started);

        SutInfoDto dto = remoteController.getSutInfo();
        assertNotNull(dto);

        baseUrlOfSut = dto.baseUrlOfSUT;
        assertNotNull(baseUrlOfSut);

        System.out.println("Remote controller running on port "+ controllerPort);
        System.out.println("SUT listening on "+baseUrlOfSut);
    }

    @AfterAll
    public static void tearDown() {

        boolean stopped = remoteController.stopSUT();
        stopped = embeddedStarter.stop() && stopped;

        assertTrue(stopped);
    }


    @BeforeEach
    public void initTest() {

        boolean reset = remoteController.resetSUT();
        assertTrue(reset);
    }
}
