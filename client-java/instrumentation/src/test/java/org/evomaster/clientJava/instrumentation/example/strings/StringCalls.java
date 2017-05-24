package org.evomaster.clientJava.instrumentation.example.strings;

public interface StringCalls {

    boolean callEquals(String a, Object b);

    boolean callEqualsIgnoreCase(String a, String b);

    boolean callStartsWith(String a, String b);

    boolean callStartsWith(String a, String b, int toffset);

    boolean callEndsWith(String a, String b);

    boolean callIsEmpty(String a);

    boolean callContentEquals(String a, CharSequence cs);

    boolean callContentEquals(String a, StringBuffer sb);

    boolean callContains(String a, CharSequence cs);

}
