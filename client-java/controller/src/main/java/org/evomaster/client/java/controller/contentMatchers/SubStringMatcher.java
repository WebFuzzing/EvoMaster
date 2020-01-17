package org.evomaster.client.java.controller.contentMatchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class SubStringMatcher  extends TypeSafeMatcher<String> {

    private final String value;

    public SubStringMatcher(String value) { this.value = value; }

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
        if(item == null) return false;
        else return (value.contains(item) || item.contains(value));
    }

    public static Matcher<String> subStringMatches(String item) {return new SubStringMatcher((item)); }
    public static Matcher<String> subStringMatches(Object item) {return new SubStringMatcher((item.toString())); }

    public static boolean subStringsMatch(Object item1, Object item2){
        if(item1 == null || item2 == null) return false;
        SubStringMatcher s1 = new SubStringMatcher(item1.toString());
        return s1.matchesSafely(item2.toString());
    }

}
