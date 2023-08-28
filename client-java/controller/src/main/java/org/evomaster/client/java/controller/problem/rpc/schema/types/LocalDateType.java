package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.IntParam;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LocalDateType extends DateType{

    public final static String INSTANCE_LOCALDATE_OF_EPOCHDAY = "ofEpochDay";

    public LocalDateType(String type, String fullTypeName, Class<?> clazz, JavaDtoSpec spec) {
        super(type, fullTypeName, clazz, spec);
    }

    public LocalDateType(String type, String fullTypeName, Class<?> clazz, JavaDtoSpec spec, List<IntParam> dateFields) {
        super(type, fullTypeName, clazz, spec, dateFields);
    }

    public LocalDateType(JavaDtoSpec spec) {
        this(LocalDate.class.getSimpleName(), LocalDate.class.getName(), LocalDate.class, spec);
        setDateFields(Arrays.asList(year, month, day));
    }

    @Override
    public LocalDateType copy() {
        return new LocalDateType(spec);
    }

    @Override
    public Object getDateInstance(List<IntParam> values) {
        if (values.size() != 3)
            throw new IllegalArgumentException("LocalDateType must have three integer parameters representing year, month and day of month");
        return LocalDate.of(values.get(0).getValue(), values.get(1).getValue(), values.get(2).getValue());
    }

    @Override
    public List<IntParam> getIntValues(Object date) {
        if (!(date instanceof LocalDate))
            throw new IllegalArgumentException("cannot handle the object instance which is not java.time.LocalDate");

        List<IntParam> values = getDateFields().stream().map(x-> (IntParam)x.copyStructureWithProperties()).collect(Collectors.toList());
        values.get(0).setValue(((LocalDate) date).getYear());
        values.get(1).setValue(((LocalDate) date).getMonthValue());
        values.get(2).setValue(((LocalDate) date).getDayOfMonth());

        return values;
    }

    @Override
    public long getDateLong(List<IntParam> values) {
        return ((LocalDate)getDateInstance(values)).toEpochDay();
    }

    @Override
    public String getDateString(List<IntParam> values) {
        /*
            apply ISO-8601 format uuuu-MM-dd.
            see toString() of java.time.LocalDate
         */
        return String.format("%04d-%02d-%02d",
            values.get(0).getValue(),
            values.get(1).getValue(),
            values.get(2).getValue());
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.depth = depth;
        dto.type = RPCSupportedDataType.LOCAL_DATE;
        return dto;
    }
}
