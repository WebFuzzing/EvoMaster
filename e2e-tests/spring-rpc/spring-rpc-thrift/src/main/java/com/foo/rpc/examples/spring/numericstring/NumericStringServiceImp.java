package com.foo.rpc.examples.spring.numericstring;

import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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
            lv = (new BigDecimal(value.longValue)).longValue();
            res += "LONG;";
            iv = (new BigDecimal(value.intValue)).intValue();
            res += "INT;";
            dv = (new BigDecimal(value.doubleValue)).doubleValue();
            res += "DOUBLE;";
        }catch (Exception e){
            return "ERROR;";
        }

        if (lv == 212121L)
            res += "L_FOUND;";

        if (iv == -4242)
            res += "I_FOUND;";

        if (dv < 40.42 && dv > 40.24)
            res += "D_FOUND;";

        if (dv == 0.0)
            res += "0_FOUND;";

        return res +" {"+ value +"}";
    }
}
