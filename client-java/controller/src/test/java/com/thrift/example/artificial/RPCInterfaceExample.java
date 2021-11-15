package com.thrift.example.artificial;

import java.util.List;
import java.util.Map;

/**
 * created by manzhang on 2021/11/15
 */
public interface RPCInterfaceExample {

    public void array(List<String>[] args0);

    public void list(List<String> args0);

    public void map(Map<String, String> args0);

    public void listAndMap(List<Map<String, String>> args0);

    public ObjectResponse objResponse();

}
