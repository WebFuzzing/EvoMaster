package com.thrift.example.artificial;

import java.util.List;

public class NestedGenericDto<T>{

    public GenericDto<T, Integer> intData;


    public GenericDto<T, String> stringData;

    public List<T> list;

}
