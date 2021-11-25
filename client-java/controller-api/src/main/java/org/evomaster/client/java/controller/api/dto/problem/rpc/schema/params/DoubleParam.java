package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PrimitiveOrWrapperType;

/**
 * double param
 */
public class DoubleParam extends PrimitiveOrWrapperParam<Double> {
    public DoubleParam(String name, String type, String fullTypeName) {
        super(name, type, fullTypeName);
    }

    public DoubleParam(String name, PrimitiveOrWrapperType type) {
        super(name, type);
    }
}
