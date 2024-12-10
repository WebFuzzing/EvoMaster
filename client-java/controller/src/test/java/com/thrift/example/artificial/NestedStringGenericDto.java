package com.thrift.example.artificial;

import java.util.List;

public class NestedStringGenericDto {

    public GenericDto<String, Integer> intData;


    public GenericDto<String, String> stringData;

    public List<String> list;
}
