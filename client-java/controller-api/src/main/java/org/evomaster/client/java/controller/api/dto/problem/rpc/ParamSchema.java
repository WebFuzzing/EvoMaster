package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.io.Serializable;

/**
 * created by manzhang on 2021/11/3
 */
public abstract class ParamSchema implements Serializable {
    private final String type;
    private final String fullTypeName;
    private final String name;

    /*
        TODO handle constraints
        ind1 uses javax-validation
     */

    public ParamSchema(String type, String fullTypeName, String name) {
        this.type = type;
        this.name = name;
        this.fullTypeName = fullTypeName;
    }

    public String getType(){
        return type;
    }

    public String getName() {
        return name;
    }

    public String getFullTypeName() {
        return fullTypeName;
    }

    public abstract ParamSchema copy();

}
