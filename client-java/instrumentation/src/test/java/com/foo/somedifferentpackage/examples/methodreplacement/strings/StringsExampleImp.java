package com.foo.somedifferentpackage.examples.methodreplacement.strings;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.StringClassReplacement;
import org.evomaster.client.java.instrumentation.example.methodreplacement.strings.StringsExample;

import static org.evomaster.client.java.instrumentation.shared.ObjectiveNaming.METHOD_REPLACEMENT;

public class StringsExampleImp implements StringsExample {

    @Override
    public boolean isFooWithDirectReturn(String value) {
        return "foo".equals(value);
    }

    @Override
    public boolean isFooWithDirectReturnUsingReplacement(String value){
        return new StringClassReplacement().equals("foo", value, METHOD_REPLACEMENT);
    }

    @Override
    public boolean isFooWithBooleanCheck(String value) {
        return "foo".equals(value) == true;
    }

    @Override
    public boolean isFooWithNegatedBooleanCheck(String value) {
        return "foo".equals(value) != false;
    }

    @Override
    public boolean isFooWithIf(String value) {
        if("foo".equals(value)){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isFooWithLocalVariable(String value) {

        boolean local = "foo".equals(value);

        return local;
    }

    @Override
    public boolean isFooWithLocalVariableInIf(String value) {

        boolean local;
        if("foo".equals(value)){
            local = true;
        } else {
            local = false;
        }

        return local;
    }

    @Override
    public boolean isNotFooWithLocalVariable(String value) {

        boolean local = ! "foo".equals(value);

        return local;
    }

    @Override
    public boolean isBarWithPositiveX(String value, int x) {

        boolean local = value.equals("bar") && x > 0;

        return local;
    }

}
