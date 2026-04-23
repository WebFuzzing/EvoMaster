package com.foo.rpc.examples.spring.taintignorecase;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;

@Service
public class TaintIgnoreCaseServiceImp implements TaintIgnoreCaseService.Iface{
    @Override
    public String getIgnoreCase(String value) throws TException {
        if(value.equalsIgnoreCase("A123b")
                && value.startsWith("a")
                && value.endsWith("B")){
            return value;
        }

        return "";
    }
}
