package com.foo.rest.emb.json.language;

import java.util.Objects;


/**
 * This code is taken from LanguageTool
 * G: https://github.com/languagetool-org/languagetool
 * L: LGPL-2.1
 * P: src/main/java/org/languagetool/server/RemoteRuleMatch.java
 */

/**
 * A potential error as returned by the HTTP API of LanguageTool.
 * @since 4.0
 */
class RemoteRuleMatch {

    private final String ruleId;
    private final String msg;

    RemoteRuleMatch(String ruleId, String msg) {
        this.ruleId = Objects.requireNonNull(ruleId);
        this.msg = Objects.requireNonNull(msg);
    }

    /** Unique (per language) identifier for the error. */
    public String getRuleId() {
        return ruleId;
    }

    /** A text describing the error to the user. */
    public String getMessage() {
        return msg;
    }

}
