package org.evomaster.e2etests.spring.rpc.grpc.examples;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.e2etests.utils.RPCTestBase;

public class GRPCServerTestBase extends RPCTestBase {

    protected static void initClass(EmbeddedSutController controller) throws Exception {

        RPCTestBase.initClass(controller);
    }
}
