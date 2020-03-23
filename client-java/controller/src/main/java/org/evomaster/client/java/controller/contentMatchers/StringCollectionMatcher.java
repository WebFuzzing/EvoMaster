package org.evomaster.client.java.controller.contentMatchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import java.util.Collection;

public class StringCollectionMatcher extends TypeSafeMatcher<Collection<String>> {
    private final Collection<String> stringCollection;
    private final String item;

    public StringCollectionMatcher(Collection<String> stringCollection) {
        this.stringCollection = stringCollection;
        this.item = null;
    }
    public StringCollectionMatcher(String item) {
        this.item = item;
        this.stringCollection = null;
    }
    public StringCollectionMatcher(Collection<String> stringCollection, String item) {
        this.stringCollection = stringCollection;
        this.item = item;
    }

    @Override
    protected boolean matchesSafely(Collection<String> stringCollection) {
        if (this.stringCollection == null || stringCollection == null) return false;
        else return stringCollection.containsAll(this.stringCollection) && this.stringCollection.containsAll(stringCollection);
    }

    protected boolean matchesSafely(String item){
        if(stringCollection == null || item == null) return false;
        else return stringCollection.contains(item);
    }

    public boolean isContainedIn(Collection<String> collection){
        if(this.item == null || collection == null) return false;
        else return collection.contains(item);
    }

    public boolean areContainedIn(Collection<String> collection){
        if (this.stringCollection == null || collection == null) return false;
        else return collection.containsAll(stringCollection);
    }

    public boolean contains(Collection<String> collection){
        if (this.stringCollection == null || collection == null) return false;
        else return stringCollection.containsAll(collection);
    }

    @Override
    public void describeTo(Description description) {
        //The point of the matcher is to allow comparisons between int and double that have the same valueE.g. that (int) 0 == (double) 0.0
        description.appendValue(stringCollection);
    }

    public static StringCollectionMatcher collectionContains(Collection<String> collection){
        return new StringCollectionMatcher(collection);
    }

    public static StringCollectionMatcher collectionContains(String item){
        return new StringCollectionMatcher(item);
    }

    public boolean collectionContainsItem(String item){
        if (stringCollection == null) return false;
        else return stringCollection.contains(item);
    }

    public static boolean collectionsMatch(Collection<String> firstCollection, Collection<String> secondCollection){
        if(firstCollection == null || secondCollection == null) return false;
        StringCollectionMatcher n1 = new StringCollectionMatcher(firstCollection);
        return n1.matchesSafely(secondCollection);
    }

    public static boolean collectionContains(Collection<String> stringCollection, String stringItem){
        if(stringCollection == null || stringItem == null) return false;
        StringCollectionMatcher n1 = new StringCollectionMatcher(stringCollection);
        return n1.collectionContainsItem(stringItem);
    }

    public static boolean collectionContains(Collection<String> stringCollection1, Collection<String> stringCollection2){
        if(stringCollection1 == null || stringCollection2 == null) return false;
        StringCollectionMatcher n1 = new StringCollectionMatcher(stringCollection1);
        return n1.contains(stringCollection2);
    }

    public static boolean collectionIsContained(Collection<String> stringCollection1, Collection<String> stringCollection2){
        if(stringCollection1 == null || stringCollection2 == null) return false;
        StringCollectionMatcher n1 = new StringCollectionMatcher(stringCollection1);
        return n1.areContainedIn(stringCollection2);
    }

}

