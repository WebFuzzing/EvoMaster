package com.foo.somedifferentpackage.examples.laststatement;

import org.evomaster.client.java.instrumentation.example.laststatement.LastStatement;

public class LastStatementImp implements LastStatement {

    @Override
    public void base() {
        int x = 2;
        int y = 3;
        int z = x * y;
        System.out.println(z);
    }

    @Override
    public int exceptionInMiddle(boolean exception) {

        int x = 42;
        if(exception){
            throw new IllegalStateException();
        }

        return x;
    }

    @Override
    public int exceptionInMethodInput(boolean exception) {

        int[] array = {};

        return array[exceptionInMiddle(exception)] = 2;
    }
}
