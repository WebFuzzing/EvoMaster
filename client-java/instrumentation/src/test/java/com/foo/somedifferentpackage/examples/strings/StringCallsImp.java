package com.foo.somedifferentpackage.examples.strings;

import org.evomaster.clientJava.instrumentation.example.strings.StringCalls;

public class StringCallsImp implements StringCalls {
    @Override
    public boolean callEquals(String a, Object b) {
        if(a.equals(b))
            return true;
        return false;
    }

    @Override
    public boolean callEqualsIgnoreCase(String a, String b) {
        if(a.equalsIgnoreCase(b))
            return true;
        return false;
    }

    @Override
    public boolean callStartsWith(String a, String b) {
        if(a.startsWith(b))
            return true;
        return false;
    }

    @Override
    public boolean callStartsWith(String a, String b, int toffset) {
        if(a.startsWith(b, toffset))
            return true;
        return false;
    }

    @Override
    public boolean callEndsWith(String a, String b) {
        if(a.endsWith(b))
            return true;
        return false;
    }

    @Override
    public boolean callIsEmpty(String a) {
        if(a.isEmpty())
            return true;
        return false;
    }

    @Override
    public boolean callContentEquals(String a, CharSequence cs) {
        if(a.contentEquals(cs))
            return true;
        return false;
    }

    @Override
    public boolean callContentEquals(String a, StringBuffer sb) {
        if(a.contentEquals(sb))
            return true;
        return false;
    }

    @Override
    public boolean callContains(String a, CharSequence cs) {
        if(a.contains(cs))
            return true;
        return false;
    }
}
