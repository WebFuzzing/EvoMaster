package org.evomaster.client.java.instrumentation.example.methodreplacement.strings;

public interface StringsExample {

    boolean isFooWithDirectReturn(String value);

    boolean isFooWithDirectReturnUsingReplacement(String value);

    boolean isFooWithBooleanCheck(String value);

    boolean isFooWithNegatedBooleanCheck(String value);

    boolean isFooWithIf(String value);

    boolean isFooWithLocalVariable(String value);

    boolean isFooWithLocalVariableInIf(String value);

    boolean isNotFooWithLocalVariable(String value);

    boolean isBarWithPositiveX(String value, int x);
}
