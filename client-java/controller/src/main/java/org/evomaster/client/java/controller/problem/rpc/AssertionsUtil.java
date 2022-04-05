package org.evomaster.client.java.controller.problem.rpc;

import java.util.Arrays;
import java.util.List;

public class AssertionsUtil {

    private final static List<String> skipKeywords_en = Arrays.asList("time", "token", "random","date","hour", "minute","second");
    private final static List<String> skipKeywords_ch = Arrays.asList("秘钥", "随机");

    /**
     * there might exist dependent or random values
     * they could be changed over time, then for such, we might avoid
     * creating assertions for it, in this case, we still generate such
     * assertions but comment them out
     * @param assertionScript an assertion
     * @return if comment it out
     */
    public static boolean getAssertionsWithComment(String assertionScript){
        if (assertionScript == null) return false;
        return skipKeywords_en.stream().anyMatch(k-> assertionScript.toLowerCase().contains(k))
                || skipKeywords_ch.stream().anyMatch(assertionScript::contains);
    }
}
