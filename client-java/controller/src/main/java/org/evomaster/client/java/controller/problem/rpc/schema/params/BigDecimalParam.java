package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.BigDecimalType;

import java.math.BigDecimal;
import java.util.List;

public class BigDecimalParam extends NamedTypedValue<BigDecimalType, BigDecimal>{


    public BigDecimalParam(String name, BigDecimalType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    public BigDecimalParam(String name, AccessibleSchema accessibleSchema){
        this(name, new BigDecimalType(), accessibleSchema);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        return null;
    }

    @Override
    public NamedTypedValue<BigDecimalType, BigDecimal> copyStructure() {
        return null;
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {

    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {

    }

    @Override
    public List<String> newInstanceWithJava(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent) {
        return null;
    }

    @Override
    public List<String> newAssertionWithJava(int indent, String responseVarName, int maxAssertionForDataInCollection) {
        return null;
    }

    @Override
    public String getValueAsJavaString() {
        return null;
    }
}
