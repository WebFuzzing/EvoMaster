package org.evomaster.e2etests.spring.examples;

import org.evomaster.clientJava.controller.SutController;
import org.evomaster.e2etests.utils.RestTestBase;

public class SpringTestBase extends RestTestBase {

    protected static void initClass(SutController controller) throws Exception {

        RestTestBase.initClass(controller);
    }
}