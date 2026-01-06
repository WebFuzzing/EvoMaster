package com.foo.somedifferentpackage.examples.positiveinteger;

import com.foo.rest.examples.spring.positiveinteger.PositiveInteger;

public class PositiveIntegerImp implements PositiveInteger {

    @Override
    public boolean isPositive(int x) {
        if(x > 0) {
            return true;
        }
        return false;
    }
}
