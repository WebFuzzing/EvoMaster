package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;

import java.math.BigInteger;

public class BigIntegerType extends TypeSchema {

    private final static String BIGINTEGER_TYPE_NAME = BigInteger.class.getSimpleName();
    private final static String FULL_BIGINTEGER_TYPE_NAME = BigInteger.class.getName();


    public BigIntegerType(JavaDtoSpec spec) {
        super(BIGINTEGER_TYPE_NAME, FULL_BIGINTEGER_TYPE_NAME, BigInteger.class, spec);
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.type = RPCSupportedDataType.BIGINTEGER;
        return dto;
    }

    @Override
    public BigDecimalType copy() {
        return new BigDecimalType(spec);
    }
}
