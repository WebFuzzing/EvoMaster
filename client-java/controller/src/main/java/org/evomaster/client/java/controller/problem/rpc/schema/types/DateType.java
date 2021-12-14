package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;
import org.evomaster.client.java.controller.problem.rpc.schema.params.IntParam;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class DateType extends TypeSchema {

    public final boolean EMPLOY_SIMPLE_Format;

    public final IntParam year = new IntParam("year");
    public final IntParam month = new IntParam("month");
    public final IntParam day = new IntParam("day");
    public final IntParam hour = new IntParam("hour");
    public final IntParam minute = new IntParam("minute");
    public final IntParam second = new IntParam("second");
    public final IntParam millisecond = new IntParam("millisecond");
    public final IntParam timezone = new IntParam("timezone");
    public final List<IntParam> dateFields;

    /**
     * simple date format
     * current default setting for handling date
     */
    public final static SimpleDateFormat SIMPLE_DATE_FORMATTER =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * complete date format which could conform with time long value
     *
     * note that
     * if we employ this format, we need to extend time gene for supporting millisecond and timezone
     */
    public final static SimpleDateFormat DATE_FORMATTER =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ZZZZ");

    public DateType(String type, String fullTypeName, Class<?> clazz, boolean simpleFormat) {
        super(type, fullTypeName, clazz);
        EMPLOY_SIMPLE_Format = simpleFormat;
        if (EMPLOY_SIMPLE_Format)
            dateFields = Arrays.asList(year, month, day, hour, minute, second);
        else
            dateFields = Arrays.asList(year, month, day, hour, minute, second, millisecond, timezone);

    }

    public DateType(String type, String fullTypeName, Class<?> clazz) {
        this(type, fullTypeName, clazz, true);
    }

    public DateType(){
        this(Date.class.getSimpleName(), Date.class.getName(), Date.class);
    }

    public List<IntParam> getDateFields(){
        return dateFields;
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

    public String getDateString(List<IntParam> values){
        if (values.size() != dateFields.size())
            throw new RuntimeException("mismatched size of values, it should be "+dateFields.size() + ", but it is "+values.size());
        if (EMPLOY_SIMPLE_Format)
            return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                    values.get(0).getValue(),
                    values.get(1).getValue(),
                    values.get(2).getValue(),
                    values.get(3).getValue(),
                    values.get(4).getValue(),
                    values.get(5).getValue()
            );

        return String.format("%04d-%02d-%02d %02d:%02d:%02d.%03d %s",
                values.get(0).getValue(),
                values.get(1).getValue(),
                values.get(2).getValue(),
                values.get(3).getValue(),
                values.get(4).getValue(),
                values.get(5).getValue(),
                values.get(6).getValue(),
                formatZZZZ(values.get(7).getValue())
        );
    }

    public long getDateLong(List<IntParam> values){
        return getDateInstance(values).getTime();
    }

    private String formatZZZZ(int zone){
        int value = zone;
        if (zone < 0)
            value = value * -1;

        String stringValue = String.format("%04d", value);
        if (zone < 0)
            stringValue = "-"+stringValue;
        else
            stringValue = "+"+stringValue;
        return stringValue;
    }

    public List<IntParam> getIntValues(Date date){
        String stringValue = DATE_FORMATTER.format(date);
        String[] strValues = stringValue.split(" ");
        assert strValues.length == 3;

        List<IntParam> values = dateFields.stream().map(IntParam::copyStructure).collect(Collectors.toList());
        //date
        String[] dateValues = strValues[0].split("-");
        assert dateValues.length == 3;
        values.get(0).setValue(Integer.parseInt(dateValues[0]));
        values.get(1).setValue(Integer.parseInt(dateValues[1]));
        values.get(2).setValue(Integer.parseInt(dateValues[2]));

        //time
        String[] timeValues = strValues[1].split(":");
        assert timeValues.length == 3;
        values.get(3).setValue(Integer.parseInt(timeValues[0]));
        values.get(4).setValue(Integer.parseInt(timeValues[1]));

        String[] secondValue = timeValues[2].split("\\.");
        assert secondValue.length == 2;
        values.get(5).setValue(Integer.parseInt(secondValue[0]));
        if (!EMPLOY_SIMPLE_Format){
            values.get(6).setValue(Integer.parseInt(secondValue[1]));

            //timezone
            values.get(7).setValue(Integer.parseInt(strValues[2]));
        }

        return values;
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.depth = depth;
        dto.type = RPCSupportedDataType.UTIL_DATE;
        return dto;
    }
}
