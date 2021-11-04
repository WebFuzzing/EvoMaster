package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * created by manzhang on 2021/11/3
 */
public class EnumParamSchema extends ParamSchema{
    private final List<String> items;

    public final static String ENUM_TYPE_NAME = "enum";

    public EnumParamSchema(String name, List<String> items) {
        super(ENUM_TYPE_NAME, ENUM_TYPE_NAME, name);
        this.items = items;
    }

    @Override
    public ParamSchema copy() {
        return new EnumParamSchema(getName(), new ArrayList<>(items));
    }
}
