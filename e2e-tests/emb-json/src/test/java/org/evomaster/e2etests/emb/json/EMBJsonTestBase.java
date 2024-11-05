package org.evomaster.e2etests.emb.json;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.e2etests.utils.RestTestBase;

public class EMBJsonTestBase extends RestTestBase {

    protected static void initClass(EmbeddedSutController controller) throws Exception {
        RestTestBase.initClass(controller);
    }

}
