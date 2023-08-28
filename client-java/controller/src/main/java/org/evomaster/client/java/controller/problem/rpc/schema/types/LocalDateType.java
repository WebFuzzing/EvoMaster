package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.IntParam;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class LocalDateType extends DateType{
    public LocalDateType(String type, boolean simpleFormat, JavaDtoSpec spec) {
        super(type, LocalDate.class.getName(), LocalDate.class, simpleFormat, spec);
    }

    public LocalDateType(String type, JavaDtoSpec spec) {
        super(type, LocalDate.class.getName(), LocalDate.class, spec);
    }

    public LocalDateType(JavaDtoSpec spec) {
        super(LocalDate.class.getSimpleName(), LocalDate.class.getName(), LocalDate.class, spec);
    }


    @Override
    public LocalDateType copy() {
        return new LocalDateType(spec);
    }

    @Override
    public Object getDateInstance(List<IntParam> values) {
        return null;
    }

    @Override
    public List<IntParam> getIntValues(Object date) {
        return null;
    }

    @Override
    public long getDateLong(List<IntParam> values) {
        return 0;
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.depth = depth;
        dto.type = RPCSupportedDataType.LOCAL_DATE;
        return dto;
    }
}
