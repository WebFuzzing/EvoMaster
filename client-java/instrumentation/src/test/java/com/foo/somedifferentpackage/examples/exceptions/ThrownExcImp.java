package com.foo.somedifferentpackage.examples.exceptions;

import org.evomaster.client.java.instrumentation.example.exceptions.ThrownExc;

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

    @Override
    public String inConstructor(boolean doThrow) {
        new Foo(doThrow);
        return "foo";
    }


    private static class Foo{
        public Foo(boolean doThrow){
            if(doThrow)
                throw new RuntimeException();
        }
    }
}
