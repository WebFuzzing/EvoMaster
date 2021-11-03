package org.evomaster.client.java.controller.problem.rpc;

import java.io.Serializable;

/**
 * created by manzhang on 2021/11/3
 */
public abstract class ParamSchema implements Serializable {
    private final String type;
    private final String name;


    public ParamSchema(String type, String name) {
        this.type = type;
        this.name = name;
    }
}
