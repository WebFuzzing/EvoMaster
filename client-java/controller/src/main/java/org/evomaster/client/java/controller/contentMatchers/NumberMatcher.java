package org.evomaster.client.java.controller.contentMatchers;

import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class NumberMatcher extends TypeSafeMatcher<Number> {
    private final double value;

    public NumberMatcher(double value) {
        this.value = value;
    }

    @Override
    public void describeTo(Description description) {
        //The point of the matcher is to allow comparisons between int and double that have the same valueE.g. that (int) 0 == (double) 0.0
        description.appendValue(value);
    }

    @Override
    protected boolean matchesSafely (Number item) {
        if (item == null) return false;
        else return item.doubleValue() == value;
    }
    public static NumberMatcher numberMatches(Number item) {
        return new NumberMatcher(item.doubleValue());
    }
    public static NumberMatcher numberMatches(String item) {
        try{
            Number value = Double.parseDouble(item);
            return new NumberMatcher(value.doubleValue());
        }
        catch(NumberFormatException e){
            return new NumberMatcher(Double.NaN); // this should not match
        }
    }
    public static boolean numbersMatch(Number item1, Number item2){
        if (item1 == null || item2 == null) return false;
        NumberMatcher n1 = new NumberMatcher(item1.doubleValue());
        return n1.matchesSafely(item2);
    }

    public static boolean numbersMatch(Number item1, String item2){
        if(item1 == null || item2 == null) return false;
        NumberMatcher n2 = numberMatches(item2);
        return n2.matchesSafely(item1);
    }


}
