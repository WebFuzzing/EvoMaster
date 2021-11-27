package com.thrift.example.artificial;

import java.util.List;
import java.util.Map;

/**
 * created by manzhang on 2021/11/15
 */
public interface RPCInterfaceExample {

    public GenericResponse array(List<String>[] args0);

    public GenericResponse arrayboolean(boolean[] args0);

    public GenericResponse list(List<String> args0);

    public GenericResponse map(Map<String, String> args0);

    public GenericResponse listAndMap(List<Map<String, String>> args0);

    public ObjectResponse objResponse();

}
