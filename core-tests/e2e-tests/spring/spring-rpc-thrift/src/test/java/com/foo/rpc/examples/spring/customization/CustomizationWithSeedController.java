package com.foo.rpc.examples.spring.customization;

import org.evomaster.client.java.controller.api.dto.CustomizedRequestValueDto;
import org.evomaster.client.java.controller.api.dto.KeyValuePairDto;
import org.evomaster.client.java.controller.api.dto.KeyValuesDto;

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
}
