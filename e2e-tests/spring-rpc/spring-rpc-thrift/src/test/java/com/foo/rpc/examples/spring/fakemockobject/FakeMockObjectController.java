package com.foo.rpc.examples.spring.fakemockobject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.rpc.examples.spring.SpringController;
import com.foo.rpc.examples.spring.fakemockobject.generated.FakeDatabaseRow;
import com.foo.rpc.examples.spring.fakemockobject.generated.FakeMockObjectService;
import com.foo.rpc.examples.spring.fakemockobject.generated.FakeRetrieveData;
import com.foo.rpc.examples.spring.fakemockobject.impl.FakeMockObjectApp;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.*;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.*;

public class FakeMockObjectController extends SpringController {

    private final static TSerializer serializer;
    private final static ObjectMapper mapper = new ObjectMapper();

    static {
        try {
            serializer = new TSerializer(new TSimpleJSONProtocol.Factory());
        } catch (TTransportException e) {
            throw new RuntimeException(e);
        }
    }

    private FakeMockObjectService.Client client;

    public FakeMockObjectController() {
        super(FakeMockObjectApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>() {{
            put(FakeMockObjectService.Iface.class.getName(), client);
        }}, new HashMap<String, List<String>>() {{
            put(FakeMockObjectService.Iface.class.getName(), new ArrayList<String>(Collections.singletonList("backdoor")));
        }}, null, null, null, RPCType.GENERAL);
    }

    @Override
    public String startClient() {
        String url = "http://localhost:"+getSutPort()+"/fakemockobject";
        try {
            // init client
            TTransport transport = new THttpClient(url);
            TProtocol protocol = new TBinaryProtocol(transport);
            client = new FakeMockObjectService.Client(protocol);
        } catch (TTransportException e) {
            e.printStackTrace();
        }

        return url;
    }

    @Override
    public List<SeededRPCTestDto> seedRPCTests() {

        FakeRetrieveData seededData = new FakeRetrieveData(){{
            id = 0;
            name = "foo";
            info = "2023-06-16";
        }};

        FakeDatabaseRow seededRow = new FakeDatabaseRow(){{
             id = 42;
             name = "bar";
             info = "2023-06-16";
        }};


        String seededDataJson = null;
        String seededRowJson = null;
        try {
            seededDataJson = serializer.toString(seededData);
            seededRowJson = serializer.toString(seededRow);
        } catch (TException e) {
            throw new RuntimeException(e);
        }

        String finalSeededDataJson = seededDataJson;
        String finalSeededRowJson = seededRowJson;
        return Arrays.asList(
                new SeededRPCTestDto(){{
                    testName = "test_1";
                    rpcFunctions = Arrays.asList(
                            new SeededRPCActionDto(){{
                                interfaceName = FakeMockObjectService.Iface.class.getName();
                                functionName = "getFooFromExternalService";
                                inputParams= Arrays.asList("0");
                                inputParamTypes= Arrays.asList(int.class.getName());
                                mockRPCExternalServiceDtos= Arrays.asList(
                                        new MockRPCExternalServiceDto(){{
                                            appKey = "fake.app";
                                            interfaceFullName = "fake.interface";
                                            functionName = "fake.func";
                                            responses = Arrays.asList(finalSeededDataJson);
                                            responseTypes = Arrays.asList(
                                                    FakeRetrieveData.class.getName()
                                            );
                                        }}
                                );
                            }}
                    );
                }},

                new SeededRPCTestDto(){{
                    testName = "test_2";
                    rpcFunctions = Arrays.asList(
                            new SeededRPCActionDto(){{
                                interfaceName = FakeMockObjectService.Iface.class.getName();
                                functionName = "getBarFromDatabase";
                                inputParams= Arrays.asList("42");
                                inputParamTypes= Arrays.asList(int.class.getName());
                                mockDatabaseDtos = Arrays.asList(
                                        new MockDatabaseDto(){{
                                            appKey = "fake.app";
                                            commandName = "fake.command";
                                            response = finalSeededRowJson;
                                            responseFullType = FakeDatabaseRow.class.getName();
                                        }}
                                );
                            }}
                    );
                }}
        );
    }

    @Override
    public boolean customizeMockingDatabase(List<MockDatabaseDto> databaseDtos, boolean enabled) {
        try {
            if (enabled){
                boolean ok = true;
                for (MockDatabaseDto dto: databaseDtos){
                    if (dto.response!= null && dto.responseFullType != null){
                        Class clazz = Class.forName(dto.responseFullType);
                        FakeDatabaseRow data = (FakeDatabaseRow) mapper.readValue(dto.response, clazz);
                        ok = ok && client.backdoor(null, data);
                    }
                }
                return ok;
            }else {
                return client.backdoor(null, null);
            }
        } catch (TException | ClassNotFoundException | JsonProcessingException e) {
            return false;
        }
    }

    @Override
    public boolean customizeMockingRPCExternalService(List<MockRPCExternalServiceDto> externalServiceDtos, boolean enabled) {

        try {
            if (enabled){
                boolean ok = true;
                for (MockRPCExternalServiceDto dto: externalServiceDtos){
                    if (dto.responses!= null && !dto.responses.isEmpty() && dto.responses.size() == dto.responseTypes.size()){
                        Class clazz = Class.forName(dto.responseTypes.get(0));
                        FakeRetrieveData data = (FakeRetrieveData) mapper.readValue(dto.responses.get(0), clazz);
                        ok = ok && client.backdoor(data, null);
                    }
                }
                return ok;
            }else {
                return client.backdoor(null, null);
            }
        } catch (TException | ClassNotFoundException | JsonProcessingException e) {
            return false;
        }
    }
}

