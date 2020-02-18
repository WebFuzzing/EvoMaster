package org.evomaster.client.java.controller.contentMatchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import java.util.Collection;

public class StringCollectionMatcher extends TypeSafeMatcher<Collection<String>> {
    private final Collection<String> value;

    public StringCollectionMatcher(Collection<String> value) {
        this.value = value;
    }

    @Override
    protected boolean matchesSafely(Collection<String> stringCollection) {
        if (value == null || stringCollection == null) return false;
        else return stringCollection.containsAll(value) && value.containsAll(stringCollection);
    }

    @Override
    public void describeTo(Description description) {
        //The point of the matcher is to allow comparisons between int and double that have the same valueE.g. that (int) 0 == (double) 0.0
        description.appendValue(value);
    }

    public boolean collectionContains(String item){
        if (value == null) return false;
        else return value.contains(item);
    }

    public static boolean collectionsMatch(Collection<String> firstCollection, Collection<String> secondCollection){
        if(firstCollection == null || secondCollection == null) return false;
        StringCollectionMatcher n1 = new StringCollectionMatcher(firstCollection);
        return n1.matchesSafely(secondCollection);
    }

    public static boolean collectionContains(Collection<String> stringCollection, String stringItem){
        if(stringCollection == null || stringItem == null) return false;
        StringCollectionMatcher n1 = new StringCollectionMatcher(stringCollection);
        return n1.collectionContains(stringItem);
    }

}

