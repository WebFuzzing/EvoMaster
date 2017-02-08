package com.foo.somedifferentpackage.examples.positiveinteger;

import org.evomaster.clientJava.instrumentation.example.positiveinteger.PositiveInteger;

public class PositiveIntegerImp implements PositiveInteger {

    @Override
    public boolean isPositive(int x) {
        if(x > 0) {
            return true;
        }
        return false;
    }
}
