package com.foo.rpc.examples.spring.numericstring;

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

public class NumericStringWithSeedTestController extends SpringController {

    private NumericStringService.Client client;

    public NumericStringWithSeedTestController(){
        super(NumericStringApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>() {{
            put(NumericStringService.Iface.class.getName(), client);
        }});
    }

    @Override
    public String startClient() {
        String url = "http://localhost:"+getSutPort()+"/numericstring";
        try {
            // init client
            TTransport transport = new THttpClient(url);
            TProtocol protocol = new TBinaryProtocol(transport);
            client = new NumericStringService.Client(protocol);
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
                                interfaceName = NumericStringService.Iface.class.getName();
                                functionName = "getNumber";
                                inputParams= Arrays.asList("{\"longValue\":\"212121\",\"intValue\":\"-4242\",\"doubleValue\":\"40.40\"}");
                                inputParamTypes= Arrays.asList(StringDto.class.getName());
                            }}
                    );
                }},
                new SeededRPCTestDto(){{
                    testName = "test_2";
                    rpcFunctions = Arrays.asList(
                            new SeededRPCActionDto(){{
                                interfaceName = NumericStringService.Iface.class.getName();
                                functionName = "getNumber";
                                inputParams= Arrays.asList("{\"longValue\":\"212121\",\"intValue\":\"-4242\",\"doubleValue\":\"0.0\"}");
                                inputParamTypes= Arrays.asList(StringDto.class.getName());
                            }}
                    );
                }}
        );
    }
}
