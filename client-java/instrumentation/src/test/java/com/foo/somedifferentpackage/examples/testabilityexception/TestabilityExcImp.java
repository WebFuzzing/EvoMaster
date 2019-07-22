package com.foo.somedifferentpackage.examples.testabilityexception;

import org.evomaster.client.java.instrumentation.example.testabilityexception.TestabilityExc;

import java.time.LocalDate;

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
}
