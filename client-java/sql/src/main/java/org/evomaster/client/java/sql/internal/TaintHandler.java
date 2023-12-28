package org.evomaster.client.java.sql.internal;

public interface TaintHandler {

    void handleTaintForStringEquals(String left, String right, boolean ignoreCase);

    void handleTaintForRegex(String value, String regex);
}
