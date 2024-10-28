package com.foo.rest.emb.json.genome;

import java.util.ArrayList;
import java.util.List;

/**
 * This code is taken from Genome Nexus
 * G: https://github.com/genome-nexus/genome-nexus
 * L: MIT
 * P: src/main/java/org/cbioportal/genome_nexus/util/Transformer.java
 */
public class Transformer
{
    /**
     * Transforms the given raw JSON into a list of DBObject instances.
     *
     * @param rawJson    raw json value
     * @return List of DBObject instances
     */
    public static List<DBObject> convertToDbObjectList(DBObject rawJson)
    {
        List<DBObject> dbObjects = new ArrayList<>();

        // if it is a list, just add all into the list
        if (rawJson instanceof List)
        {
            for (Object obj: ((List) rawJson))
            {
                dbObjects.add(new DBObject());
//                dbObjects.add(new BasicDBObject((Map) obj));
            }
        }
        else
        {
            dbObjects.add(rawJson);
        }

        return dbObjects;
    }
}
