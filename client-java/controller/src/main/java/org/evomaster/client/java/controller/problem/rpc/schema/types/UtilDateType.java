package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.IntParam;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class UtilDateType extends DateType{
    public UtilDateType(String type, boolean simpleFormat, JavaDtoSpec spec) {
        super(type, Date.class.getName(), Date.class, simpleFormat, spec);
    }

    public UtilDateType(String type, JavaDtoSpec spec) {
        super(type, Date.class.getName(), Date.class, spec);
    }

    public UtilDateType(JavaDtoSpec spec) {
        super(Date.class.getSimpleName(), Date.class.getName(), Date.class, spec);
    }


    public Date getDateInstance(List<IntParam> values){
        String stringValue = getDateString(values);
        try {
            if (EMPLOY_SIMPLE_Format)
                return SIMPLE_DATE_FORMATTER.parse(stringValue);
            else
                return DATE_FORMATTER.parse(stringValue);
        } catch (ParseException e) {
            throw new RuntimeException("ERROR: fail to parse values to Date");
        }
    }

    @Override
    public List<IntParam> getIntValues(Object date){
        if (!(date instanceof Date))
            throw new IllegalArgumentException("cannot handle the object instance which is not java.util.Date");

        String stringValue = DATE_FORMATTER.format(date);
        String[] strValues = stringValue.split(" ");
        if (strValues.length != 3){
            throw new IllegalArgumentException("invalid a string for specifying a date:"+ stringValue);
        }

        List<IntParam> values = dateFields.stream().map(x-> (IntParam)x.copyStructureWithProperties()).collect(Collectors.toList());
        //date
        String[] dateValues = strValues[0].split("-");

        if (dateValues.length != 3){
            throw new IllegalArgumentException("invalid a string for specifying a date:"+ strValues[0]);
        }
        values.get(0).setValue(Integer.parseInt(dateValues[0]));
        values.get(1).setValue(Integer.parseInt(dateValues[1]));
        values.get(2).setValue(Integer.parseInt(dateValues[2]));

        //time
        String[] timeValues = strValues[1].split(":");
        if (timeValues.length != 3){
            throw new IllegalArgumentException("invalid a string for specifying a time:"+ strValues[1]);
        }
        values.get(3).setValue(Integer.parseInt(timeValues[0]));
        values.get(4).setValue(Integer.parseInt(timeValues[1]));

        String[] secondValue = timeValues[2].split("\\.");
        if (secondValue.length != 2){
            throw new IllegalArgumentException("invalid a string for specifying seconds:"+ strValues[2]);
        }
        values.get(5).setValue(Integer.parseInt(secondValue[0]));
        if (!EMPLOY_SIMPLE_Format){
            values.get(6).setValue(Integer.parseInt(secondValue[1]));

            //timezone
            values.get(7).setValue(Integer.parseInt(strValues[2]));
        }

        return values;
    }

    @Override
    public long getDateLong(List<IntParam> values){
        return getDateInstance(values).getTime();
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.depth = depth;
        dto.type = RPCSupportedDataType.UTIL_DATE;
        return dto;
    }

    @Override
    public UtilDateType copy() {
        return new UtilDateType(spec);
    }
}
