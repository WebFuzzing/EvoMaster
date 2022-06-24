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

/**
 * type schema for date
 */
public class DateType extends TypeSchema {

    /**
     * represent the type employs SimpleDateFormat as [SIMPLE_DATE_FORMATTER]
     */
    public final boolean EMPLOY_SIMPLE_Format;

    /**
     * year field
     */
    public final IntParam year = new IntParam("year");
    /**
     * month field
     */
    public final IntParam month = new IntParam("month");
    /**
     * day field
     */
    public final IntParam day = new IntParam("day");
    /**
     * hour field
     */
    public final IntParam hour = new IntParam("hour");
    /**
     * minute field
     */
    public final IntParam minute = new IntParam("minute");
    /**
     * second field
     */
    public final IntParam second = new IntParam("second");
    /**
     * millisecond field
     */
    public final IntParam millisecond = new IntParam("millisecond");
    /**
     * time zone field
     */
    public final IntParam timezone = new IntParam("timezone");
    /**
     * a sequence of fields representing the date
     */
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

    /**
     *
     * @param type is the type name
     * @param fullTypeName is the full type name
     * @param clazz is the class representing the type
     * @param simpleFormat specifies if use simple format as SIMPLE_DATE_FORMATTER
     */
    public DateType(String type, String fullTypeName, Class<?> clazz, boolean simpleFormat) {
        super(type, fullTypeName, clazz);
        EMPLOY_SIMPLE_Format = simpleFormat;
        if (EMPLOY_SIMPLE_Format)
            dateFields = Arrays.asList(year, month, day, hour, minute, second);
        else
            dateFields = Arrays.asList(year, month, day, hour, minute, second, millisecond, timezone);

    }
    /**
     * DateType with simpleFormat
     * @param type is the type name
     * @param fullTypeName is the full type name
     * @param clazz is the class representing the type
     *
     */
    public DateType(String type, String fullTypeName, Class<?> clazz) {
        this(type, fullTypeName, clazz, true);
    }

    /**
     * a java.util.Date with simple format
     */
    public DateType(){
        this(Date.class.getSimpleName(), Date.class.getName(), Date.class);
    }

    /**
     *
     * @return a list of date fields used in this type
     */
    public List<IntParam> getDateFields(){
        return dateFields;
    }

    /**
     *
     * @param values are a list of values for the date
     * @return a date instance based on the values
     */
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

    /**
     *
     * @param values are a list of values for the date
     * @return a string representing the date with the specified values
     */
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

    /**
     * extract value of fields based on the date instance
     * @param date is an instance of Date
     * @return a list of fields which contains specific values
     */
    public List<IntParam> getIntValues(Date date){
        String stringValue = DATE_FORMATTER.format(date);
        String[] strValues = stringValue.split(" ");
//        assert strValues.length == 3;
        if (strValues.length != 3){
            throw new IllegalArgumentException("invalid a string for specifying a date:"+ stringValue);
        }

        List<IntParam> values = dateFields.stream().map(x-> (IntParam)x.copyStructureWithProperties()).collect(Collectors.toList());
        //date
        String[] dateValues = strValues[0].split("-");
//        assert dateValues.length == 3;

        if (dateValues.length != 3){
            throw new IllegalArgumentException("invalid a string for specifying a date:"+ strValues[0]);
        }
        values.get(0).setValue(Integer.parseInt(dateValues[0]));
        values.get(1).setValue(Integer.parseInt(dateValues[1]));
        values.get(2).setValue(Integer.parseInt(dateValues[2]));

        //time
        String[] timeValues = strValues[1].split(":");
//        assert timeValues.length == 3;
        if (timeValues.length != 3){
            throw new IllegalArgumentException("invalid a string for specifying a time:"+ strValues[1]);
        }
        values.get(3).setValue(Integer.parseInt(timeValues[0]));
        values.get(4).setValue(Integer.parseInt(timeValues[1]));

        String[] secondValue = timeValues[2].split("\\.");
//        assert secondValue.length == 2;
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
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.depth = depth;
        dto.type = RPCSupportedDataType.UTIL_DATE;
        return dto;
    }


    @Override
    public DateType copy() {
        return new DateType();
    }
}
