package com.foo.rpc.examples.spring.testability;

import com.foo.rpc.examples.spring.SpringController;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCTestDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TestabilityWithSeedTestController extends SpringController {
    private TTransport transport;
    private TProtocol protocol;
    private TestabilityService.Client client;

    public TestabilityWithSeedTestController(){
        super(TestabilityApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>() {{
            put(TestabilityService.Iface.class.getName(), client);
        }});
    }

    @Override
    public String startClient() {
        String url = "http://localhost:"+getSutPort()+"/testability";
        try {
            // init client
            transport = new THttpClient(url);
            protocol = new TBinaryProtocol(transport);
            client = new TestabilityService.Client(protocol);
        } catch (TTransportException e) {
            e.printStackTrace();
        }

        return url;
    }

    @Override
    public List<SeededRPCTestDto> seedRPCTests() {
        return Arrays.asList(
                new SeededRPCTestDto(){{
                    testName = "test_1";
                    rpcFunctions = Arrays.asList(
                            new SeededRPCActionDto(){{
                                interfaceName = TestabilityService.Iface.class.getName();
                                functionName = "getSeparated";
                                inputParams= Arrays.asList("2019-01-01","42","Foo");
                                inputParamTypes= Arrays.asList(String.class.getName(),String.class.getName(),String.class.getName());
                            }}
                    );
                }}
        );
    }
}

