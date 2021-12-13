package com.thrift.example.artificial;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * created by manzhang on 2021/11/15
 */
public class ObjectResponse {

    @NotNull@NotEmpty
    public String f1;

    public int f2;

    public double f3;

    public ObjectResponse cycle;


    public double[] f4;

    public List<String>[] f5;
}
