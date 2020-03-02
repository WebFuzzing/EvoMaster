package com.foo.somedifferentpackage.examples.nonintegercomparisons;

import org.evomaster.client.java.instrumentation.example.nonintegercomparisons.NIC_Example;

/**
 * Created by arcuri82 on 02-Mar-20.
 */
public class NIC_ExampleImp implements NIC_Example {


    @Override
    public int pos(long x, long y) {
        if(x>0L){
            return 0;
        }

        if(y >= 0L){
            return 1;
        }

        return 2;
    }

    @Override
    public int neg(long x, long y) {
        if(x<0L){
            return 3;
        }

        if(y <= 0L){
            return 4;
        }

        return 5;
    }

    @Override
    public int eq(long x, long y) {
        if(x==0L) {
            return 6;
        }

        if(y!=0L) {
            return 7;
        }

        return 8;
    }

    @Override
    public int pos(double x, double y) {
        if(x>0d){
            return 0;
        }

        if(y >= 0d){
            return 1;
        }

        return 2;
    }

    @Override
    public int neg(double x, double y) {
        if(x<0d){
            return 3;
        }

        if(y <= 0d){
            return 4;
        }

        return 5;
    }

    @Override
    public int eq(double x, double y) {
        if(x==0d) {
            return 6;
        }

        if(y!=0d) {
            return 7;
        }

        return 8;
    }

    @Override
    public int pos(float x, float y) {
        if(x>0f){
            return 0;
        }

        if(y >= 0f){
            return 1;
        }

        return 2;
    }

    @Override
    public int neg(float x, float y) {
        if(x<0f){
            return 3;
        }

        if(y <= 0f){
            return 4;
        }

        return 5;
    }

    @Override
    public int eq(float x, float y) {
        if(x==0f) {
            return 6;
        }

        if(y!=0f) {
            return 7;
        }

        return 8;
    }
}
