package org.evomaster.client.java.instrumentation.example.jackson;

import java.util.List;


public interface JsonMap {


    public int castLongToInt(long l) ;

    public Integer castIntToInteger(int i);

    public int castIntegerToInt(Integer i);

    public List castToList(String json) throws Exception;

    public int castToIntArray(String json) throws Exception;

    public Integer assignedToTypedList(String json) throws Exception;

    public int castIntFromFunction(String json) throws Exception;
}
