package com.foo.somedifferentpackage.examples.exceptions;

import org.evomaster.clientJava.instrumentation.example.exceptions.ThrownExc;

public class ThrownExcImp implements ThrownExc {

    @Override
    public String directReturn(Object obj){
        return obj.toString();
    }

    @Override
    public String directInTry(Object obj){
        try{
            return obj.toString();
        } catch (Exception e){
            throw e;
        }
    }

    @Override
    public String doubleCall(Object x, Object y){
        return x.toString() + y.toString();
    }

    @Override
    public String callOnArray(Object[] array, int index){
        return array[index].toString();
    }
}
