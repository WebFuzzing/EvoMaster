package com.foo.rpc.examples.spring.thrifttest;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Service
public class SecondServiceImp implements SecondService.Iface{
    /**
     * Prints 'testString("%s")' with thing as '%s'
     *
     * @param thing@return string - returns the string 'thing'
     */
    @Override
    public String secondtestString(String thing) throws TException {
        System.out.printf("secondtestString(\"%s\")%n", thing);
        return thing;
    }
}
