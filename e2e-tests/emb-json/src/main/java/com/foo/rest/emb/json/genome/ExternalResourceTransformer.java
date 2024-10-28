package com.foo.rest.emb.json.genome;

import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;

public class ExternalResourceTransformer<T> implements ResourceTransformer<T>
{
    private final ObjectMapper externalResourceObjectMapper;

//    @Autowired
    public ExternalResourceTransformer(
            @Qualifier("defaultExternalResourceObjectMapper") ObjectMapper externalResourceObjectMapper)
    {
        this.externalResourceObjectMapper = externalResourceObjectMapper;
    }

    @Override
    public List<T> transform(DBObject json, Class<T> type) throws ResourceMappingException
    {
        return this.mapJsonToInstance(json, type, this.externalResourceObjectMapper);
    }

//    @Override
//    public List<DBObject> transform(DBObject rawJson)
//    {
//        return Transformer.convertToDbObjectList(rawJson);
//    }

    /**
     * Maps the given raw JSON DBObject onto the provided class instances.
     *
     * @param rawJson       raw JSON value
     * @param type          object class
     * @param objectMapper  custom object mapper
     * @return a list of instances of the provided class
     * @throws ResourceMappingException
     */
    private List<T> mapJsonToInstance(DBObject rawJson, Class<T> type, ObjectMapper objectMapper)
            throws ResourceMappingException
    {
        List<T> list = new ArrayList<>();
        ObjectMapper mapper = objectMapper;

        if (mapper == null)
        {
            mapper = new ObjectMapper();
        }

        try {
            for (DBObject dbObject: Transformer.convertToDbObjectList(rawJson))
            {
                // convert DBObject to a proper instance of the given class type
                list.add(mapper.convertValue(dbObject, type));
            }
        } catch (Exception e) {
            throw new ResourceMappingException(e.getMessage());
        }

        return list;
    }
}
