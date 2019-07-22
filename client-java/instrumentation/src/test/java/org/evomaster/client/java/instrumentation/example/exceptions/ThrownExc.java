package org.evomaster.client.java.instrumentation.example.exceptions;

public interface ThrownExc {

    String directReturn(Object obj);

    String directInTry(Object obj);

    String doubleCall(Object x, Object y);

    String callOnArray(Object[] array, int index);

    String inConstructor(boolean doThrow);
}
