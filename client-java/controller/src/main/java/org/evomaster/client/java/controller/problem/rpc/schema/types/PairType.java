package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;

import java.util.AbstractMap;
import java.util.Arrays;

/**
 * used AbstractMap.SimpleEntry for handling map
 */
public class PairType extends TypeSchema{

    private final static String PAIR_TYPE_NAME = AbstractMap.SimpleEntry.class.getSimpleName();
    private final static String FULL_PAIR_TYPE_NAME = AbstractMap.SimpleEntry.class.getName();
    /**
     * template of first
     */
    private final NamedTypedValue firstTemplate;
    /**
     * template of second
     */
    private final NamedTypedValue secondTemplate;

    public PairType(NamedTypedValue keyTemplate, NamedTypedValue valueTemplate, JavaDtoSpec spec) {
        super(PAIR_TYPE_NAME, FULL_PAIR_TYPE_NAME, AbstractMap.SimpleEntry.class, spec);
        this.firstTemplate = keyTemplate;
        this.secondTemplate = valueTemplate;
    }

    public NamedTypedValue getFirstTemplate() {
        return firstTemplate;
    }

    public NamedTypedValue getSecondTemplate() {
        return secondTemplate;
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        ParamDto example = new ParamDto();
        example.innerContent = Arrays.asList(firstTemplate.getDto(), secondTemplate.getDto());
        dto.example = example;
        dto.type = RPCSupportedDataType.PAIR;
        return dto;
    }

    @Override
    public PairType copy() {
        return new PairType(getFirstTemplate(), getSecondTemplate(), spec);
    }
}
