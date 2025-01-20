package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.PatternMatchingHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.*;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.regex.Matcher;

/**
 * Created by arcuri82 on 11-Sep-19.
 */
public class MatcherClassReplacement implements MethodReplacementClass {

    private static final Field textField;

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
    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean matches(Matcher caller, String idTemplate) {
        Objects.requireNonNull(caller);

        String text = getText(caller);
        String pattern = caller.pattern().toString();
        int flags = caller.pattern().flags();

        boolean patternMatchesResult = PatternMatchingHelper.matches(pattern, flags, text, idTemplate);

        TaintType taintType = ExecutionTracer.getTaintType(text);

        if (taintType.isTainted()) {
            /*
                .matches() does a full match of the text, not a partial.
             */
            String regex = caller.pattern().toString();
            ExecutionTracer.addStringSpecialization(text,
                    new StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, regex, taintType));
        }
        boolean matcherMatchesResults = caller.matches();
        assert (patternMatchesResult == matcherMatchesResults);
        return matcherMatchesResults;
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
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
          add some prefix and suffix to match the
          find
         */


        /*
            Bit tricky... (.*) before/after the regex would not work, as by default . does
            not match line terminators. enabling DOTALL flag is risky, as the original could
            use flags.
            \s\S is just a way to covering everything
         */
        TaintType taintType = ExecutionTracer.getTaintType(substring);
        if (taintType.isTainted()) {
            ExecutionTracer.addStringSpecialization(substring,
                    new StringSpecializationInfo(StringSpecialization.REGEX_PARTIAL, regex, taintType));
        }

        String anyPositionRegexMatch = RegexSharedUtils.handlePartialMatch(regex);
        boolean patternMatchResult = PatternMatchingHelper.matches(anyPositionRegexMatch, substring, idTemplate);
        boolean matcherFindResult = caller.find();
        if(patternMatchResult != matcherFindResult){
            //TODO we should analyze those cases, and fix them
            SimpleLogger.uniqueWarn("Failed to handle regex in Matcher.find(): " + regex);
        }
        return matcherFindResult;
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
            return ((CharSequence) textField.get(match)).toString();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
