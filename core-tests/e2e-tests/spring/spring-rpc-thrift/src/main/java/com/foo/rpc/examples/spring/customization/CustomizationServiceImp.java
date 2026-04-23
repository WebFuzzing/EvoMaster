package com.foo.rpc.examples.spring.customization;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CustomizationServiceImp implements CustomizationService.Iface{

    private final Map<String, String> codeCheck = new HashMap<String, String>(){{
        put("foo", "foo_passcode");
        put("bar", "bar_passcode");
    }};

    @Override
    public int handleDependent(RequestWithSeedDto dto) throws TException {
        if (dto.value == 0.42)
            return 1;
        if (dto.value == 42.42)
            return 43;
        if (dto.value == 100.42)
            return 101;
        return 0;
    }

    @Override
    public int handleCombinedSeed(RequestWithCombinedSeedDto dto) throws TException {
        if (codeCheck.get(dto.requestId) == null || !codeCheck.get(dto.requestId).equals(dto.requestCode))
            return -1;
        if (dto.value == 0.42)
            return 1;
        if (dto.value == 42.42)
            return 43;
        if (dto.value == 100.42)
            return 101;
        return 0;
    }
}
