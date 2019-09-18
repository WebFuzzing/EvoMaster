package org.evomaster.client.java.instrumentation.example.methodreplacement;

import java.text.DateFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Created by arcuri82 on 27-Jun-19.
 */
public interface TestabilityExc {

    // Collection
    boolean contains(Collection instance, Object o);
    boolean isEmpty(Collection instance);

    // Date
    boolean after(Date instance, Date when);
    boolean before(Date instance, Date when);
    boolean equals(Date instance, Date other);

    // DateFormat
    Date dateFormatParse(DateFormat caller, String input) throws ParseException;

    // Integer
    int parseInt(String input);

    // LocalDate
    LocalDate parseLocalDate(String input);

    // Map
    boolean containsKey(Map instance, Object o);

    // Objects
    boolean objectEquals(Object left, Object right);

    // Boolean
    boolean parseBoolean(String input);

    // Long
    long parseLong(String input);

    // Float
    float parseFloat(String input);

    // Double
    double parseDouble(String input);

    // String
    boolean stringEquals(String caller, Object input);
    boolean stringEqualsIgnoreCase(String caller, String input);
    boolean stringIsEmpty(String caller);
    boolean contentEquals(String caller, CharSequence input);
    boolean contentEquals(String caller, StringBuffer input);
    boolean contains(String caller, CharSequence input);
    boolean startsWith(String caller, String prefix);
    boolean startsWith(String caller, String prefix, int toffset);


}
