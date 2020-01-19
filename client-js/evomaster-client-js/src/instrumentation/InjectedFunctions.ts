/*
    This is a bit different from Controller in Java.
    In Java, each class is uniquely identified by its package, which is
    what used in the bytecode, where "import" in the source code is just
    syntactic sugar.
    But here in JavaScript we need to import a module in the instrumented files,
    and the import itself must be added as part of the instrumentation.
    To avoid mess in scope resolutions, all EM functions that can be called/injected
    in the SUT will be defined here
 */


import ExecutionTracer from "./staticstate/ExecutionTracer";

export default class InjectedFunctions {

    public static enteringStatement(fileName: string, line: number, statementId: number) {
        ExecutionTracer.enteringStatement(fileName, line, statementId)
    }

    public static completedStatement(fileName: string, line: number, statementId: number) {
        ExecutionTracer.completedStatement(fileName, line, statementId)
    }


    public static completingStatement(value: any, fileName: string, line: number, statementId: number) : any {
        ExecutionTracer.completedStatement(fileName, line, statementId)
        return value
    }

}