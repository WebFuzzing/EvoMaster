package com.foo.somedifferentpackage.examples.branches;

import org.evomaster.clientJava.instrumentation.example.branches.Branches;

public class BranchesImp implements Branches{

    @Override
    public int pos(int x, int y) {

        if(x>0){
            return 0;
        }

        if(y >= 0){
            return 1;
        }

        return 2;
    }

    @Override
    public int neg(int x, int y) {
        if(x<0){
            return 3;
        }

        if(y <= 0){
            return 4;
        }

        return 5;
    }

    @Override
    public int eq(int x, int y) {

        if(x==0) {
            return 6;
        }

        if(y!=0) {
            return 7;
        }

        return 8;
    }


}
