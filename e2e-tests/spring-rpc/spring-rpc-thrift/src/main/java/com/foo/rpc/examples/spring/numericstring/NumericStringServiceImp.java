package com.foo.rpc.examples.spring.numericstring;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

@Service
public class NumericStringServiceImp implements NumericStringService.Iface{
    @Override
    public String getNumber(StringDto value) throws TException {

        String res = "";
        if (value == null || value.longValue == null || value.intValue == null || value.doubleValue == null)
            return "NULL";

        long lv;
        int iv;
        double dv;
        try {
            lv = Long.parseLong(value.longValue);
            res += "LONG;";
            iv = Integer.parseInt(value.intValue);
            res += "INT;";
            dv = Double.parseDouble(value.doubleValue);
            res += "DOUBLE;";
        }catch (Exception e){
            return "ERROR;";
        }

        if (lv == 212121L)
            res += "L_FOUND;";

        if (iv == 4242)
            res += "I_FOUND;";

        if (dv == 4040.42)
            res += "D_FOUND;";

        if (dv == 0.0)
            res += "0_FOUND;";
        return res;
    }
}
