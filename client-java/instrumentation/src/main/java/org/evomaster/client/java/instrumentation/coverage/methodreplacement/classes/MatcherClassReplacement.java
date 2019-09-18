package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.PatternMatchingHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by arcuri82 on 11-Sep-19.
 */
public class MatcherClassReplacement implements MethodReplacementClass {

    private static Field textField = null;

    static {
        try {
            textField = Matcher.class.getDeclaredField("text");
            textField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Class<?> getTargetClass() {
        return Matcher.class;
    }


    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean matches(Matcher caller, String idTemplate) {

        if (caller == null) {
            caller.matches();
        }

        Pattern pattern = caller.pattern();
        String input = getText(caller);
        /*
            .matches() does a full match of the text, not a partial.

            TODO: enclosing the pattern in ^(pattern)$ would be fine for most
            cases, but not fully correct: eg for multi-lines, and if pattern
            already has ^ and $
        */
        String regex = "^(" + pattern.toString() + ")$";

        return PatternMatchingHelper.matches(regex, input, idTemplate);
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean find(Matcher caller, String idTemplate) {

        if (caller == null) {
            caller.find();
        }

        Pattern pattern = caller.pattern();
        String input = getText(caller);
        int end = caller.end();

        /*
            .matches() does a full match of the text, not a partial.

            TODO: enclosing the pattern in ^(pattern)$ would be fine for most
            cases, but not fully correct: eg for multi-lines, and if pattern
            already has ^ and $
        */
        String regex = "^(" + pattern.toString() + ")$";

        /*
            As find() is not idempotent, instead of directly calling
            find(), we compute the substring and use the matches()
            helper on the substring.
         */
        String substring = input.substring(end);
        return PatternMatchingHelper.matches(regex, substring, idTemplate);
    }


    /**
     * Since a Matcher instance has no way of
     * accessing the original text for the matching,
     * we need to access the private fields
     *
     * @param match
     * @return
     */
    private static String getText(Matcher match) {
        try {
            return (String) textField.get(match);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
