package org.evomaster.client.java.controller.contentMatchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Arrays;
import java.util.Collection;

public class StringCollectionMatcher extends TypeSafeMatcher<Collection<String>> {
    private final Collection<String> value;

    public StringCollectionMatcher(Collection<String> value) {
        this.value = value;
    }

    @Override
    protected boolean matchesSafely(Collection<String> item) {
        return item.containsAll(value) && value.containsAll(item);
    }

    @Override
    public void describeTo(Description description) {
        //The point of the matcher is to allow comparisons between int and double that have the same valueE.g. that (int) 0 == (double) 0.0
        description.appendValue(value);
    }

    public boolean collectionContains(String item){
        return value.contains(item);
    }

    public static boolean collectionsMatch(Collection<String> item1, Collection<String> item2){
        if(item1 == null || item2 == null) return false;
        StringCollectionMatcher n1 = new StringCollectionMatcher(item1);
        return n1.matchesSafely(item2);
    }

    public static boolean collectionContains(Collection<String> item1, String item2){
        if(item1 == null || item2 == null) return false;
        StringCollectionMatcher n1 = new StringCollectionMatcher(item1);
        return n1.collectionContains(item2);
    }

}

