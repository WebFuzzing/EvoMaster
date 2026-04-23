namespace EvoMaster.Instrumentation.Examples.Strings{
    public interface IStringsExample{
        bool isFooWithDirectReturn(string value);

        // EvoMaster.Instrumentation.Examples could be instrumented for tests
        // then it cannot refer to EvoMaster.Instrumentation
        // bool isFooWithDirectReturnUsingReplacement(string value);

        bool isFooWithBooleanCheck(string value);

        bool isFooWithNegatedBooleanCheck(string value);

        bool isFooWithIf(string value);

        bool isFooWithLocalVariable(string value);

        bool isFooWithLocalVariableInIf(string value);

        bool isNotFooWithLocalVariable(string value);

        bool isBarWithPositiveX(string value, int x);
    }
}