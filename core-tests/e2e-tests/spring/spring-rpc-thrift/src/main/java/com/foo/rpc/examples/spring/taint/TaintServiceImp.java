package com.foo.rpc.examples.spring.taint;

import net.thirdparty.taint.TaintCheckString;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Service
public class TaintServiceImp implements TaintService.Iface{
    @Override
    public String getInteger(@NotNull String value) throws TException {
        int x = Integer.parseInt(value);
        return "integer " + x;
    }

    @Override
    public String getDate(@NotNull String value) throws TException {
        LocalDate x = LocalDate.parse(value);
        return "date " +x;
    }

    @Override
    public String getConstant(@NotNull String value) throws TException {
        if(! value.equals("Hello world!!! Even if this is a long string, it will be trivial to cover with taint analysis")){
            throw new IllegalArgumentException(":-(");
        }
        return "constant OK";
    }

    @Override
    public String getThirdParty(@NotNull String value) throws TException {
        if(!TaintCheckString.check(value)){
            throw new IllegalArgumentException(":-(");
        }
        return "thirdparty OK";
    }

    @Override
    public String getCollection(@NotNull String value) throws TException {
        if(!TaintCheckString.check(value)){
            throw new IllegalArgumentException(":-(");
        }
        return "collection OK";
    }
}
