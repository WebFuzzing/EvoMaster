package com.foo.rest.emb.json.genome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
