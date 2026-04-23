package com.foo.rpc.examples.spring.hypermutation;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;

@Service
public class HypermutationServiceImp implements HypermutationService.Iface{
    @Override
    public String differentWeight(@NotNull int x, @NotNull String y, HighWeightDto z) throws TException {
        if (z == null) return null;
        String response = "";
        if (x == 42){
            response = "x";
        }
        if (y.equalsIgnoreCase("foo")){
            response += "y";
        }
        if (z.f3.equals("2021-06-17")){
            response += "z";
        }

        return response;
    }

    @Override
    public String lowWeightHighCoverage(@NotNull int x, @NotNull String y, HighWeightDto z) throws TException {
        if (z == null) return null;
        String response = "";
        if (x == 42){
            response = "x1";
        }else if (x == 100){
            response = "x2";
        }else if(x == 500){
            response = "x3";
        }else if(x == 1000){
            response = "x4";
        }else if(x == 10000){
            response = "x5";
        }
        if (y.equalsIgnoreCase("foo")){
            response += "y";
        }
        if (z.f3.equals("2021-06-17")){
            response += "z";
        }
        return response;
    }
}
