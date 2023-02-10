package com.foo.rpc.examples.spring.customization;

import org.evomaster.client.java.controller.api.dto.CustomizedRequestValueDto;
import org.evomaster.client.java.controller.api.dto.KeyValuePairDto;
import org.evomaster.client.java.controller.api.dto.KeyValuesDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCActionDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.SeededRPCTestDto;

import java.util.Arrays;
import java.util.List;

public class CustomizationWithSeedController extends CustomizationController{

    @Override
    public List<CustomizedRequestValueDto> getCustomizedValueInRequests() {
        return Arrays.asList(
                new CustomizedRequestValueDto(){{
                    combinedKeyValuePairs = Arrays.asList(
                            new KeyValuePairDto(){{
                                fieldKey = "requestId";
                                fieldValue = "foo";
                            }},
                            new KeyValuePairDto(){{
                                fieldKey = "requestCode";
                                fieldValue = "foo_passcode";
                            }}
                    );
                }},
                new CustomizedRequestValueDto(){{
                    combinedKeyValuePairs = Arrays.asList(
                            new KeyValuePairDto(){{
                                fieldKey = "requestId";
                                fieldValue = "bar";
                            }},
                            new KeyValuePairDto(){{
                                fieldKey = "requestCode";
                                fieldValue = "bar_passcode";
                            }}
                    );
                }},
                new CustomizedRequestValueDto(){{
                    keyValues = new KeyValuesDto(){{
                        key = "value";
                        values = Arrays.asList("0.42", "42.42", "100.42");
                    }};
                }}
        );
    }

    @Override
    public List<SeededRPCTestDto> seedRPCTests() {
        return Arrays.asList(
                new SeededRPCTestDto(){{
                    testName = "test_1";
                    rpcFunctions = Arrays.asList(
                            new SeededRPCActionDto(){{
                                interfaceName = CustomizationService.Iface.class.getName();
                                functionName = "handleCycleDto";
                                inputParams= Arrays.asList("{\"aID\":\"a\",\"obj\":{\"bID\":\"ab\",\"obj\":{\"aID\":\"aba\",\"obj\":{\"bID\":\"abab\",\"obj\":{\"aID\":\"ababa\",\"obj\":null}}}}}");
                                inputParamTypes= Arrays.asList(CycleADto.class.getName());
                            }}
                    );
                }}
        );
    }
}
