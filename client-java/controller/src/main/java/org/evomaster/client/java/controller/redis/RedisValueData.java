package org.evomaster.client.java.controller.redis;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;

/**
 * This class will hold the data associated to the value for a given key in Redis.
 *
 *  Fields or members will be set depending on the type of key.
 *  String keys may have a String value in the future, but currently it is not needed to store that information.
 *  Set keys will have the set members. Hash keys will have the fields.
 *  JSON keys will have the parsed JSON document, needed to navigate JSONPath expressions
 *  (e.g. JSON.GET/JSON.ARRLEN/JSON.ARRINDEX and similar commands).
 */
public class RedisValueData {
    private Map<String, String> fields;
    private Set<String> members;
    private JsonNode jsonValue;

    public RedisValueData(Map<String, String> fields) {
        this.fields = fields;
    }

    public RedisValueData(Set<String> members) {
        this.members = members;
    }

    public RedisValueData(JsonNode jsonValue) {
        this.jsonValue = jsonValue;
    }


    public Set<String> getMembers() {
        return members;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public JsonNode getJsonValue() {
        return jsonValue;
    }
}