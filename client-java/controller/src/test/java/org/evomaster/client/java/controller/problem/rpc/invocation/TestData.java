package org.evomaster.client.java.controller.problem.rpc.invocation;

import com.thrift.example.artificial.GenericDto;
import com.thrift.example.artificial.NestedGenericDto;
import com.thrift.example.artificial.NestedStringGenericDto;

import java.util.ArrayList;
import java.util.HashSet;

public class TestData {

    public static final NestedStringGenericDto NESTED_STRING_GENERIC_DTO = new NestedStringGenericDto(){{
        intData = new GenericDto<String, Integer>(){{
            data1 = "stringInt";
            data2 = 42;
        }};
        stringData = new GenericDto<String, String>(){{
            data1 = "stringString";
            data2 = "foo";
        }};
        list = new ArrayList<String>(){{
            add("bar");
        }};
    }};



    public static final NestedGenericDto<String> NESTED_GENERIC_DTO = new NestedGenericDto(){{
        intData = new GenericDto<String, Integer>(){{
            data1 = "stringInt";
            data2 = 42;
        }};
        stringData = new GenericDto<String, String>(){{
            data1 = "stringString";
            data2 = "foo";
        }};
        list = new ArrayList<String>(){{
            add("foo");
            add("bar");
        }};

        set = new HashSet(){{
            add("foo");
            add("bar");
        }};
    }};
}
