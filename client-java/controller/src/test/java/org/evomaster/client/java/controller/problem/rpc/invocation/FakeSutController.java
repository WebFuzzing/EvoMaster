package org.evomaster.client.java.controller.problem.rpc.invocation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thrift.example.artificial.*;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.auth.LocalAuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.MockRPCExternalServiceDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCTestDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.*;

/**
 * created by manzhang on 2021/11/27
 */
public class FakeSutController extends EmbeddedSutController {

    public boolean running;
    private RPCInterfaceExampleImpl sut = new RPCInterfaceExampleImpl();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String startSut() {
        if (sut == null)
            sut = new RPCInterfaceExampleImpl();
        running = true;
        return null;
    }

    @Override
    public void stopSut() {
        running =false;
    }

    @Override
    public void resetStateOfSUT() {

    }

    @Override
    public boolean isSutRunning() {
        return running;
    }

    @Override
    public String getPackagePrefixesToCover() {
        return null;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                new AuthenticationDto(){{
                    name = "local";
                    localAuthSetup = new LocalAuthenticationDto(){{
                        authenticationInfo = "local_foo";
                    }};
                }}
        );
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>(){{
            put(RPCInterfaceExample.class.getName(), sut);
        }});
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public boolean handleLocalAuthenticationSetup(String authenticationInfo) {
        boolean auth =  authenticationInfo.equals("local_foo");
        sut.setAuthorized(auth);
        return auth;
    }

    @Override
    public List<SeededRPCTestDto> seedRPCTests() {
        String mockedResponse_test1;
        String seed_requests_test3;
        try {
            mockedResponse_test1 = objectMapper.writeValueAsString(TestData.NESTED_STRING_GENERIC_DTO);
            seed_requests_test3 = objectMapper.writeValueAsString(TestData.NESTED_GENERIC_DTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return Arrays.asList(
                new SeededRPCTestDto(){{
                    testName = "test_1";
                    rpcFunctions = Arrays.asList(
                            new SeededRPCActionDto(){{
                                interfaceName = RPCInterfaceExample.class.getName();
                                functionName = "seedcheck";
                                inputParams= Arrays.asList("[1,2,3]","[1,2,3]","[{\"bdPositiveFloat\":10.12,\"bdNegativeFloat\":-10.12,\"bdPositiveOrZeroFloat\":0.00,\"bdNegativeOrZeroFloat\":-2.16,\"biPositive\":10,\"biPositiveOrZero\":0,\"biNegative\":-10,\"biNegativeOrZero\":-2}]","{\"1\":\"1\",\"2\":\"2\"}","null");
                                inputParamTypes= Arrays.asList(List.class.getName(),List.class.getName(),List.class.getName(), Map.class.getName(), BigNumberObj.class.getName());
                                mockRPCExternalServiceDtos = Arrays.asList(
                                        new MockRPCExternalServiceDto(){{
                                            this.appKey = "seedcheck.fake.mock.appkey";
                                            this.interfaceFullName = "seedcheck.fake.mock.interfaceName";
                                            this.responseTypes = Arrays.asList(NestedStringGenericDto.class.getName());
                                            this.responses= Arrays.asList(mockedResponse_test1);

                                        }}
                                );
                            }}
                    );
                }},
                new SeededRPCTestDto(){{
                    testName = "test_2";
                    rpcFunctions = Arrays.asList(
                            new SeededRPCActionDto(){{
                                interfaceName = RPCInterfaceExample.class.getName();
                                functionName = "seedcheck";
                                inputParams= Arrays.asList("null","null","null","null","null");
                                inputParamTypes= Arrays.asList(List.class.getName(),List.class.getName(),List.class.getName(), Map.class.getName(), BigNumberObj.class.getName());
                            }}
                    );
                }},
                new SeededRPCTestDto(){{
                    testName = "test_3";
                    rpcFunctions = Arrays.asList(
                        new SeededRPCActionDto(){{
                            interfaceName = RPCInterfaceExample.class.getName();
                            functionName = "handleNestedGenericString";
                            inputParams= Arrays.asList(seed_requests_test3);
                            inputParamTypes= Arrays.asList(NestedGenericDto.class.getName());
                        }}
                    );
                }}

        );
    }
}
