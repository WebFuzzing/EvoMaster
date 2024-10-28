package com.foo.rest.emb.json.genome;

import java.util.List;

public interface ResourceTransformer<T>
{
    List<T> transform(DBObject value, Class<T> type) throws ResourceMappingException;
//    List<DBObject> transform(DBObject value);
}
