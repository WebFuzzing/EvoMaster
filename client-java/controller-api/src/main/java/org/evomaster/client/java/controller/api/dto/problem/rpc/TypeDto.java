package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * created by manzhang on 2021/11/27
 */
public class TypeDto {
    /**
     * full type name
     */
    public String fullTypeName;

    /**
     * extracted type
     */
    public RPCSupportedDataType type;

    /**
     *  an example of the type
     */
    public ParamDto example;

    /**
     * representing depth to leaf type
     */
    public int depth;

    /**
     * fixed items in this types
     */
    public String[] fixedItems;
}

