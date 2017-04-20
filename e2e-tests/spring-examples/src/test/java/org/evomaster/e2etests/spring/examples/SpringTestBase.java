package org.evomaster.e2etests.spring.examples;

import org.evomaster.clientJava.controller.EmbeddedSutController;
import org.evomaster.e2etests.utils.RestTestBase;

public class SpringTestBase extends RestTestBase {

    protected static void initClass(EmbeddedSutController controller) throws Exception {

        RestTestBase.initClass(controller);
    }
}