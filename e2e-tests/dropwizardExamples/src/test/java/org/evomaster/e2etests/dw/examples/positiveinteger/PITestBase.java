package org.evomaster.e2etests.dw.examples.positiveinteger;

import org.evomaster.clientJava.controller.EmbeddedStarter;
import org.evomaster.clientJava.controllerApi.SutInfoDto;
import org.evomaster.core.problem.rest.RemoteController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class PITestBase {

    protected static EmbeddedStarter embeddedStarter;
    protected static String baseUrl;
    protected static RemoteController remoteController;
    protected static int controllerPort;

    @BeforeAll
    public static void initClass() {

        PIController controller = new PIController();
        embeddedStarter = new EmbeddedStarter(controller);
        embeddedStarter.start();

        controllerPort = embeddedStarter.getControllerServerJettyPort();

        remoteController = new RemoteController("localhost", controllerPort);
        boolean started = remoteController.startSUT();
        assertTrue(started);

        SutInfoDto dto = remoteController.getInfo();
        assertNotNull(dto);

        baseUrl = dto.baseUrlOfSUT;
        assertNotNull(baseUrl);
    }

    @AfterAll
    public static void tearDown() {

        boolean stopped = remoteController.stopSUT();
        assertTrue(stopped);
    }


    @BeforeEach
    public void initTest() {

        boolean reset = remoteController.resetSUT();
        assertTrue(reset);
    }
}
