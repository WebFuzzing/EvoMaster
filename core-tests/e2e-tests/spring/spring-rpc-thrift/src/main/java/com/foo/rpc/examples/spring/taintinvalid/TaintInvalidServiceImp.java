package com.foo.rpc.examples.spring.taintinvalid;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TaintInvalidServiceImp implements TaintInvalidService.Iface{
    public final static List list = Arrays.asList("bar", "/", "", "", "", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..", "/", ".", "//", "..");

    @Override
    public String get(String value) throws TException {
        if(list.contains(value)){
            return value;
        } else {
            return "foo";
        }
    }
}
