package org.evomaster.client.java.controller.api.dto.problem.rpc;

/**
 * a dto to collect info of type of the param of endpoints to be tested
 */
public class TypeDto {
    /**
     * full type name
     */
    public String fullTypeName;

    /**
     * full type name with generic types if it has
     */
    public String fullTypeNameWithGenericType;
    /**
     * extracted type for the param
     */
    public RPCSupportedDataType type;

    /**
     *  an example of the type
     *  eg, for generic type of list, set, array
     */
    public ParamDto example;

    /**
     * representing depth to leaf type
     */
    public int depth;

    /**
     * fixed items in this types
     * eg, items for Enum
     */
    public String[] fixedItems;

    /**
     * precision for numeric parameter if specified
     * negative number means the precision is not specified
     */
    public int precision = -1;
}

