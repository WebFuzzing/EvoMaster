package org.evomaster.e2etests.spring.mysql.entity;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.e2etests.utils.RestTestBase;

public class SpringTestBase extends RestTestBase {

    protected static void initClass(EmbeddedSutController controller) throws Exception {

        RestTestBase.initClass(controller);
    }
}
