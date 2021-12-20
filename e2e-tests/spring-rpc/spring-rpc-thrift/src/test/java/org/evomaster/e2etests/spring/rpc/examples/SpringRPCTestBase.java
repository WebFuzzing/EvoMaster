package org.evomaster.e2etests.spring.rpc.examples;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.e2etests.utils.RPCTestBase;

public class SpringRPCTestBase  extends RPCTestBase {

    protected static void initClass(EmbeddedSutController controller) throws Exception {

        RPCTestBase.initClass(controller);
    }
}
