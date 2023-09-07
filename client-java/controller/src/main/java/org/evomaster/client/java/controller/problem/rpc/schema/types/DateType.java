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
    private List<IntParam> dateFields;

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

    public DateType(String type, String fullTypeName, Class<?> clazz, JavaDtoSpec spec) {
        super(type, fullTypeName, clazz, spec);
    }

    public DateType(String type, String fullTypeName, Class<?> clazz, JavaDtoSpec spec, List<IntParam> dateFields) {
        this(type, fullTypeName, clazz, spec);
        setDateFields(dateFields);
    }

    protected void setDateFields(List<IntParam> fields){
        this.dateFields = fields;
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
    public abstract String getDateString(List<IntParam> values);


    protected String formatZZZZ(int zone){
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
