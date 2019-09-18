package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by arcuri82 on 11-Sep-19.
 */
public class MatcherClassReplacement implements MethodReplacementClass {

    private static Field textField = null;

    static{
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
    public static boolean matches(Matcher caller, String idTemplate){

        if (caller == null) {
            caller.matches();
        }

        Pattern pattern = caller.pattern();
        String text = getText(caller);

        if(ExecutionTracer.isTaintInput(text)){
            /*
                .matches() does a full match of the text, not a partial.

                TODO: enclosing the pattern in ^(pattern)$ would be fine for most
                cases, but not fully correct: eg for multi-lines, and if pattern
                already has ^ and $
             */
            String regex = "^(" + pattern.toString() + ")$";
            ExecutionTracer.addStringSpecialization(text,
                    new StringSpecializationInfo(StringSpecialization.REGEX, regex));
        }

        if (idTemplate == null) {
            return caller.matches();
        }

        //TODO branch distance computation
        return caller.matches();
    }

    private static String getText(Matcher match) {
        try {
            return (String) textField.get(match);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
