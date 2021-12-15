package com.thrift.example.artificial;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * created by manzhang on 2021/11/15
 */
public interface RPCInterfaceExample {

    public String simplePrimitive(int argInt, float argfloat, long arglong, double argdouble, char argchar, byte argbyte, boolean argboolean, short argshort);

    public String simpleWrapPrimitive(Integer argInt, Float argfloat, Long arglong, Double argdouble, Character argchar, Byte argbyte, Boolean argboolean, Short argshort);

    public GenericResponse array(List<String>[] args0);

    public GenericResponse arrayboolean(boolean[] args0);

    public GenericResponse list(List<String> args0);

    public GenericResponse map(Map<String, String> args0);

    public GenericResponse listAndMap(List<Map<String, String>> args0);

    public ObjectResponse objResponse();

    public CycleAObj objCycleA();

    public CycleBObj objCycleB();

    public String dateToString(Date date);

    public String constraintInputs(ConstrainedRequest arg0, @NotNull String arg1);

}
