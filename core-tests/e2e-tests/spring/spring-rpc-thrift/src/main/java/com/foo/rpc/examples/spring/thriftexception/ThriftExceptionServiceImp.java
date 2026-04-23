package com.foo.rpc.examples.spring.thriftexception;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Service
public class ThriftExceptionServiceImp implements ThriftExceptionService.Iface{

    @Override
    public String check(String value) throws BadResponse, ErrorResponse, TException {
        if (value.equals("foo"))
            throw new BadResponse(400, "bad response: foo");
        if (value.equals(""))
            throw new ErrorResponse(505, "error response: empty");
        int i = Integer.parseInt(value);
        return value+" number:"+i;
    }
}
