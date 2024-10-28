package com.foo.rest.emb.json.genome;

import java.util.List;

/**
 * This code is taken from Genome Nexus
 * G: https://github.com/genome-nexus/genome-nexus
 * L: MIT
 * P: src/main/java/org/cbioportal/genome_nexus/service/ResourceTransformer.java
 */
public interface ResourceTransformer<T>
{
    List<T> transform(DBObject value, Class<T> type) throws ResourceMappingException;
//    List<DBObject> transform(DBObject value);
}
