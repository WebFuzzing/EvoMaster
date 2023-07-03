package org.evomaster.client.java.controller.problem.rpc.grpc;

import io.grpc.examples.evotests.datatypes.DataTypesTestGrpc;
import org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilderTestBase;

public class DataTypesTestgRPCEndpointsBuilderTest extends RPCEndpointsBuilderTestBase {
    @Override
    public String getInterfaceName() {
        return DataTypesTestGrpc.DataTypesTestBlockingStub.class.getName();
    }

    @Override
    public int expectedNumberOfEndpoints() {
        return 7;
    }
}
