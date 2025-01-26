package com.foo.rpc.examples.spring.fakemockobject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.foo.rpc.examples.spring.SpringController;
import com.foo.rpc.examples.spring.fakemockobject.generated.*;
import com.foo.rpc.examples.spring.fakemockobject.impl.FakeMockObjectApp;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.api.dto.MockDatabaseDto;
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
                                            interfaceFullName = "com.foo.rpc.examples.spring.fakemockobject.external.fake.api.GetApiData";
                                            functionName = "one";
                                            responses = Arrays.asList("{\"exName\":\"foo\",\"exId\":0,\"exInfo\":[\"2023-08-14\"]}");
                                            responseTypes = Arrays.asList(
                                                    "com.foo.rpc.examples.spring.fakemockobject.external.fake.api.ExApiDto"
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
                                            commandName = "com.foo.rpc.examples.spring.fakemockobject.external.fake.db.GetDbData.one";
                                            response = "{\"exName\":\"bar\",\"exId\":42,\"exInfo\":[\"2023-08-14\"]}";
                                            responseFullType = "com.foo.rpc.examples.spring.fakemockobject.external.fake.db.ExDbDto";
                                        }}
                                );
                            }}
                    );
                }},

            new SeededRPCTestDto(){{
                testName = "test_3";
                rpcFunctions = Arrays.asList(
                    new SeededRPCActionDto(){{
                        interfaceName = FakeMockObjectService.Iface.class.getName();
                        functionName = "getAllBarFromDatabase";
                        inputParams= Arrays.asList();
                        inputParamTypes= Arrays.asList();
                        mockDatabaseDtos = Arrays.asList(
                            new MockDatabaseDto(){{
                                appKey = "fake.app";
                                commandName = "com.foo.rpc.examples.spring.fakemockobject.external.fake.db.GetDbData.all";
                                response = "[]";
                                responseFullType = ArrayList.class.getName();
                            }}
                        );
                    }}
                );
            }},
            new SeededRPCTestDto(){{
                testName = "test_4";
                rpcFunctions = Arrays.asList(
                        new SeededRPCActionDto(){{
                            interfaceName = FakeMockObjectService.Iface.class.getName();
                            functionName = "isExecutedToday";
                            inputParams= Arrays.asList();
                            inputParamTypes= Arrays.asList();
                        }}
                );
                scheduleTaskInvocations = Arrays.asList(
                        new ScheduleTaskInvocationDto(){{
                            appKey = "fake.app";
                            taskName = "executeFlag";
                            descriptiveInfo = "a scheduled task for invoking executeFlag";
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
                        JsonNode json = mapper.readTree(dto.response);
                        if (json instanceof ArrayNode){
                            for (JsonNode j : json){
                                FakeDatabaseRow data = new FakeDatabaseRow();
                                if (j.has("exId"))
                                    data.id = Integer.parseInt(j.get("exId").asText());
                                if (j.has("exName"))
                                    data.name = j.get("exName").asText();
                                else
                                    data.name = j.asText();
                                if (j.has("exInfo") && j.get("exInfo") instanceof ArrayNode){
                                    if (!j.get("exInfo").isEmpty()){
                                        data.info = j.get("exInfo").asText();
                                    }
                                }
                                ok = ok && client.backdoor(null, data, null);
                            }
                        }else {
                            FakeDatabaseRow data = new FakeDatabaseRow();
                            if (json.has("exId"))
                                data.id = Integer.parseInt(json.get("exId").asText());
                            if (json.has("exName"))
                                data.name = json.get("exName").asText();
                            if (json.has("exInfo") && json.get("exInfo") instanceof ArrayNode){
                                if (!json.get("exInfo").isEmpty()){
                                    data.info = json.get("exInfo").asText();
                                }
                            }
                            ok = ok && client.backdoor(null, data, null);
                        }
                    }
                }
                return ok;
            }else {
                return client.backdoor(null, null, null);
            }
        } catch (TException | JsonProcessingException e) {
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
                        JsonNode json = mapper.readTree(dto.responses.get(0));
                        FakeRetrieveData data = new FakeRetrieveData();
                        if (json.has("exId"))
                            data.id = Integer.parseInt(json.get("exId").asText());
                        if (json.has("exName"))
                            data.name = json.get("exName").asText();
                        if (json.has("exInfo") && json.get("exInfo") instanceof ArrayNode){
                            if (!json.get("exInfo").isEmpty()){
                                data.info = json.get("exInfo").asText();
                            }
                        }
                        ok = ok && client.backdoor(data, null, null);
                    }
                }
                return ok;
            }else {
                return client.backdoor(null, null, null);
            }
        } catch (TException | JsonProcessingException e) {
            return false;
        }
    }

    @Override
    public ScheduleTaskInvocationResultDto customizeScheduleTaskInvocation(ScheduleTaskInvocationDto invocationDto, boolean invoked) {
        ScheduleTaskInvocationResultDto dto = new ScheduleTaskInvocationResultDto();
        dto.status = ExecutionStatusDto.FAILED;
        dto.taskName = invocationDto.taskName;
        try {
            if (invocationDto != null && invoked){
                FakeScheduleTaskData data = new FakeScheduleTaskData();
                data.name = invocationDto.taskName;
                data.info = invocationDto.descriptiveInfo;
                data.id = System.nanoTime();
                data.startTime = String.valueOf(System.nanoTime());
                data.state = FakeScheduleTaskState.PROCESSING;
                boolean ok = client.backdoor(null, null, data);
                dto.status = ExecutionStatusDto.RUNNING;
                dto.invocationId = String.valueOf(data.id);
                if (ok) dto.status = ExecutionStatusDto.COMPLETED;
            }
        } catch (TException e) {
            dto.status = ExecutionStatusDto.FAILED;
        }

        return dto;
    }

    @Override
    public boolean isScheduleTaskCompleted(ScheduleTaskInvocationResultDto invocationInfo) {
        return true;
    }
}

