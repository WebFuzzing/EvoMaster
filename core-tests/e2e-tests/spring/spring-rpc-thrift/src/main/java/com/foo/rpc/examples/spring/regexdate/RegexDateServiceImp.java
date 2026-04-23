package com.foo.rpc.examples.spring.regexdate;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class RegexDateServiceImp implements RegexDateService.Iface{
    @Override
    public String get(String date, String seq) throws TException {
        String success = LocalDate.parse(date).toString() + "-" + Integer.parseInt(seq);
        return "OK";
    }
}
