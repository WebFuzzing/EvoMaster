package org.evomaster.client.java.instrumentation.object.dtos;

import java.util.Collection;

public class DtoCollection {

    private Collection<Boolean> collection;
    private Collection collection_raw;


    public Collection getCollection_raw() {
        return collection_raw;
    }

    public void setCollection_raw(Collection collection_raw) {
        this.collection_raw = collection_raw;
    }

    public Collection<Boolean> getCollection() {
        return collection;
    }

    public void setCollection(Collection<Boolean> collection) {
        this.collection = collection;
    }
}
