package com.foo.rest.examples.spring.positiveinteger;

public class PositiveIntegerImp implements PositiveInteger {

    @Override
    public boolean isPositive(int x) {
        if(x > 0) {
            return true;
        }
        return false;
    }
}
