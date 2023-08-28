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
public abstract class DateType extends TypeSchema {

    /**
     * represent the type employs SimpleDateFormat as [SIMPLE_DATE_FORMATTER]
     */
    public final boolean EMPLOY_SIMPLE_Format;

    /**
     * year field
     */
    public final IntParam year = new IntParam("year", spec);
    /**
     * month field
     */
    public final IntParam month = new IntParam("month", spec);
    /**
     * day field
     */
    public final IntParam day = new IntParam("day", spec);
    /**
     * hour field
     */
    public final IntParam hour = new IntParam("hour", spec);
    /**
     * minute field
     */
    public final IntParam minute = new IntParam("minute", spec);
    /**
     * second field
     */
    public final IntParam second = new IntParam("second", spec);
    /**
     * millisecond field
     */
    public final IntParam millisecond = new IntParam("millisecond", spec);
    /**
     * time zone field
     */
    public final IntParam timezone = new IntParam("timezone", spec);
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
     * @param type         is the type name
     * @param fullTypeName is the full type name
     * @param clazz        is the class representing the type
     * @param simpleFormat specifies if use simple format as SIMPLE_DATE_FORMATTER
     * @param spec
     */
    public DateType(String type, String fullTypeName, Class<?> clazz, boolean simpleFormat, JavaDtoSpec spec) {
        super(type, fullTypeName, clazz, spec);
        EMPLOY_SIMPLE_Format = simpleFormat;
        if (EMPLOY_SIMPLE_Format)
            dateFields = Arrays.asList(year, month, day, hour, minute, second);
        else
            dateFields = Arrays.asList(year, month, day, hour, minute, second, millisecond, timezone);

    }
    /**
     * DateType with simpleFormat
     *
     * @param type         is the type name
     * @param fullTypeName is the full type name
     * @param clazz        is the class representing the type
     * @param spec         is dto specification
     */
    public DateType(String type, String fullTypeName, Class<?> clazz, JavaDtoSpec spec) {
        this(type, fullTypeName, clazz, true, spec);
    }

    /**
     * a java.util.Date with simple format
     */
//    public DateType(JavaDtoSpec spec){
//        this(Date.class.getSimpleName(), Date.class.getName(), Date.class, spec);
//    }

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
    public abstract Object getDateInstance(List<IntParam> values);


    /**
     * extract value of fields based on the date instance
     * @param date is an instance of Date, eg, java.util.Date or java.time.LocalDate
     * @return a list of fields which contains specific values
     */
    public abstract List<IntParam> getIntValues(Object date);


    /**
     * @param values
     * @return date with long format
     */
    public abstract long getDateLong(List<IntParam> values);


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


}
