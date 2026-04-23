package com.foo.rest.emb.json.language;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

/**
 * This code is taken from LanguageTool
 * G: https://github.com/languagetool-org/languagetool
 * L: LGPL-2.1
 * P: src/main/java/org/languagetool/server/ResultExtender.java
 */
public class ResultExtender {

    private final ObjectMapper mapper = new ObjectMapper();


    List<RemoteRuleMatch> getExtensionMatches(String jsonString) throws IOException {
        return parseJson(jsonString);
    }


    private List<RemoteRuleMatch> parseJson(String inputStream) throws IOException {
        Map map = mapper.readValue(inputStream, Map.class);
        List matches = (ArrayList) map.get("matches");
        List<RemoteRuleMatch> result = new ArrayList<>();
        for (Object match : matches) {
            RemoteRuleMatch remoteMatch = getMatch((Map<String, Object>) match);
            result.add(remoteMatch);
        }
        return result;
    }

    private RemoteRuleMatch getMatch(Map<String, Object> match) {
        Map<String, Object> rule = (Map<String, Object>) match.get("rule");

        RemoteRuleMatch remoteMatch = new RemoteRuleMatch(getRequiredString(rule, "id"), getRequiredString(match, "message"));

        return remoteMatch;
    }

    private Object getRequired(Map<String, Object> elem, String propertyName) {
        Object val = elem.get(propertyName);
        if (val != null) {
            return val;
        }
        throw new RuntimeException("JSON item " + elem + " doesn't contain required property '" + propertyName + "'");
    }

    private String getRequiredString(Map<String, Object> elem, String propertyName) {
        return (String) getRequired(elem, propertyName);
    }

}
