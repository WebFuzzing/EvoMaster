package com.foo.somedifferentpackage.examples.methodreplacement;

import org.evomaster.client.java.instrumentation.example.methodreplacement.TestabilityExc;

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
