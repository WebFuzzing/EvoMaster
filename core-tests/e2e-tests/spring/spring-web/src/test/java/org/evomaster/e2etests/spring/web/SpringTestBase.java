package org.evomaster.e2etests.spring.web;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.e2etests.utils.WebTestBase;

public class SpringTestBase extends WebTestBase {

    protected static void initClass(EmbeddedSutController controller) throws Exception {

        WebTestBase.initClass(controller);
    }
}