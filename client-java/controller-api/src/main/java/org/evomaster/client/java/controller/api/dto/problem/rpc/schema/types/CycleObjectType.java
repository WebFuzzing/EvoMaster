package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params.NamedTypedValue;

import java.util.ArrayList;
import java.util.List;

/**
 * created by manzhang on 2021/11/15
 */
public class CycleObjectType extends ObjectType{

    public CycleObjectType(String type, String fullTypeName) {
        super(type, fullTypeName, new ArrayList<>());
    }

}
