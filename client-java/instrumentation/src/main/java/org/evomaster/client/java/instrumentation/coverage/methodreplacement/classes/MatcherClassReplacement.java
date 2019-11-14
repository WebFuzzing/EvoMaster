package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.PatternMatchingHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.*;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.regex.Matcher;

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


    /**
     * Matcher.matches() is not pure (updates last matching info)
     *
     * @param caller
     * @param idTemplate
     * @return
     */
    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean matches(Matcher caller, String idTemplate) {

        if (caller == null) {
            caller.matches();
        }
        String text = getText(caller);
        String pattern = caller.pattern().toString();

        boolean patternMatchesResult = PatternMatchingHelper.matches(pattern, text, idTemplate);

        TaintType taintType = ExecutionTracer.getTaintType(text);

        if (taintType.isTainted()) {
            /*
                .matches() does a full match of the text, not a partial.

                TODO: enclosing the pattern in ^(pattern)$ would be fine for most
                cases, but not fully correct: eg for multi-lines, and if pattern
                already has ^ and $
             */
            String regex = "^(" + caller.pattern().toString() + ")$";
            ExecutionTracer.addStringSpecialization(text,
                    new StringSpecializationInfo(StringSpecialization.REGEX, regex, taintType));
        }
        boolean matcherMatchesResults = caller.matches();
        assert (patternMatchesResult == matcherMatchesResults);
        return matcherMatchesResults;
    }

    @Replacement(type = ReplacementType.BOOLEAN)
    public static boolean find(Matcher caller, String idTemplate) {
        Objects.requireNonNull(caller);

        String input = getText(caller);
        String regex = caller.pattern().toString();
        int end;
        try {
            end = caller.end();
        } catch (IllegalStateException ex) {
            // No match available. Therefore, we kept the entire input
            end = 0;
        }

        /*
            As find() is not idempotent, instead of directly calling
            find(), we compute the substring and use the matches()
            helper on the substring.
         */
        String substring = input.substring(end);
        /*
          Since matches() requires all the input to
          match the regex, and find() only requires
          the input to appear at least once, we could
          add some prefix and sufix to match the
          find
         */


        String anyPositionRegexMatch = String.format("(.*)(%s)(.*)", regex);
        TaintType taintType = ExecutionTracer.getTaintType(substring);
        if (taintType.isTainted()) {
            /*
                .matches() does a full match of the text, not a partial.

                TODO: enclosing the pattern in ^(pattern)$ would be fine for most
                cases, but not fully correct: eg for multi-lines, and if pattern
                already has ^ and $
             */
            ExecutionTracer.addStringSpecialization(substring,
                    new StringSpecializationInfo(StringSpecialization.REGEX, anyPositionRegexMatch, taintType));
        }

        boolean patternMatchResult = PatternMatchingHelper.matches(anyPositionRegexMatch, substring, idTemplate);
        boolean matcherFindResult = caller.find();
        assert (patternMatchResult == matcherFindResult);
        return matcherFindResult;
    }


    /**
     * Since a MatTestabilityExcInstrumentedTestcher instance has no way of
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
