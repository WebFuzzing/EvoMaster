package org.evomaster.client.java.controller.contentMatchers;

import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class NumberMatcher extends TypeSafeMatcher<Number> {
    private double value;

    public NumberMatcher(double value) {
        this.value = value;
    }

    @Override
    public void describeTo(Description description) {
        //The point of the matcher is to allow comparisons between int and double that have the same valueE.g. that (int) 0 == (double) 0.0
    }

    @Override
    protected boolean matchesSafely (Number item) {
        return item.doubleValue() == value;
    }
    public static Matcher<Number> numberMatches(Number item) {
        return new NumberMatcher(item.doubleValue());
    }
    public static boolean numbersMatch(Number item1, Number item2){
        NumberMatcher n1 = new NumberMatcher(item1.doubleValue());
        return n1.matchesSafely(item2);
    }
}
