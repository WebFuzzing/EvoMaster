package org.evomaster.client.java.controller.contentMatchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class StringMatcher extends TypeSafeMatcher<String> {
    private final String value;

    public StringMatcher(String value) { this.value = value; }

    @Override
    public void describeTo(Description description){
        //The point of this matcher is to provide comparisons for strings in a way that is consistent with the
        // numerical matcher. It can also be extended to provide a more detailed type of matching
        // (for example, include settings for ignore case, include string, and anything else we might discover we need.
        // Should this prove unnecessary, it is to be removed.
        description.appendValue(value);
    }

    @Override
    protected boolean matchesSafely(String item){
        if(item == null || value == null) return false;
        else return value.equals(item.toString());
    }

    public static Matcher<String> stringMatches(String item) {return new StringMatcher((item)); }
    public static Matcher<String> stringMatches(Object item) {return new StringMatcher((item.toString())); }

    public static boolean stringsMatch(Object item1, Object item2){
        if(item1 == null || item2 == null) return false;
        StringMatcher s1 = new StringMatcher(item1.toString());
        return s1.matchesSafely(item2.toString());
    }

}

