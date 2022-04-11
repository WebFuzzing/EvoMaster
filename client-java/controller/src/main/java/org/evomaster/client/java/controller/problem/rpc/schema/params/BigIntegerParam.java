package org.evomaster.client.java.controller.problem.rpc.schema.params;


import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.BigIntegerType;

import java.math.BigInteger;
import java.util.List;

/**
 * info for JDK:
 *      BigInteger constructors and operations throw ArithmeticException when the result is
 *      out of the supported range of -2^Integer.MAX_VALUE (exclusive) to +2^Integer.MAX_VALUE (exclusive).
 */
public class BigIntegerParam extends NamedTypedValue<BigIntegerType, BigInteger> {


    public BigIntegerParam(String name, BigIntegerType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    public BigIntegerParam(String name, AccessibleSchema accessibleSchema){
        this(name, new BigIntegerType(), accessibleSchema);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        return null;
    }

    @Override
    public NamedTypedValue<BigIntegerType, BigInteger> copyStructure() {
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
