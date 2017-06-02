package com.foo.somedifferentpackage.examples.exceptions;

public class ThrownExcImp {

    public String directReturn(Object obj){
        return obj.toString();
    }

    public String directInTry(Object obj){
        try{
            return obj.toString();
        } catch (Exception e){
            throw e;
        }
    }

    public String doubleCall(Object x, Object y){
        return x.toString() + y.toString();
    }

    public String callOnArray(Object[] array, int index){
        return array[index].toString();
    }
}
