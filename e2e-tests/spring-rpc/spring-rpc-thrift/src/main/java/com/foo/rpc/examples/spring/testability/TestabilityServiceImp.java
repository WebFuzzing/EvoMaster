package com.foo.rpc.examples.spring.testability;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class TestabilityServiceImp implements TestabilityService.Iface{

    @Override
    public String getSeparated(String date, String number, String setting) throws TException {
        LocalDate d = LocalDate.parse(date);
        int n = Integer.parseInt(number);
        List<String> list = Arrays.asList("Foo", "Bar");

        if(d.getYear() == 2019 && n == 42 && list.contains(setting)){
            return "OK";
        }

        return "ERROR";
    }
}
