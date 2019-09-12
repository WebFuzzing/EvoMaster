package com.foo.somedifferentpackage.examples.methodreplacement;

import org.evomaster.client.java.instrumentation.example.methodreplacement.TestabilityExc;

import java.text.DateFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * Created by arcuri82 on 27-Jun-19.
 */
public class TestabilityExcImp implements TestabilityExc {

    @Override
    public int parseInt(String input) {
        return Integer.parseInt(input);
    }

    @Override
    public LocalDate parseLocalDate(String input) {
        return LocalDate.parse(input);
    }

    @Override
    public boolean after(Date instance, Date when) {
        return instance.after(when);
    }

    @Override
    public boolean before(Date instance, Date when) {
        return instance.before(when);
    }

    @Override
    public boolean equals(Date instance, Date other) {
        return instance.equals(other);
    }

    @Override
    public boolean isEmpty(Collection instance) {
        return instance.isEmpty();
    }

    @Override
    public boolean contains(Collection instance, Object o) {
        return instance.contains(o);
    }

    @Override
    public boolean containsKey(Map instance, Object o) {
        return instance.containsKey(o);
    }

    @Override
    public boolean objectEquals(Object left, Object right) {
        return Objects.equals(left, right);
    }

    @Override
    public Date dateFormatParse(DateFormat caller, String input) throws ParseException {
        return caller.parse(input);
    }

    @Override
    public boolean parseBoolean(String input) {
        return Boolean.parseBoolean(input);
    }

}
