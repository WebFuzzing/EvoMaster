package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;

import java.math.BigDecimal;

public class BigDecimalType extends TypeSchema {

    private final static String BIGDECIMAL_TYPE_NAME = BigDecimal.class.getSimpleName();
    private final static String FULL_BIGDECIMAL_TYPE_NAME = BigDecimal.class.getName();


    public BigDecimalType(JavaDtoSpec spec) {
        super(BIGDECIMAL_TYPE_NAME, FULL_BIGDECIMAL_TYPE_NAME, BigDecimal.class, spec);
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.type = RPCSupportedDataType.BIGDECIMAL;
        return dto;
    }

    @Override
    public BigDecimalType copy() {
        return new BigDecimalType(spec);
    }
}
