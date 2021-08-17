import {StringSpecializationInfo} from "./shared/StringSpecializationInfo";
import ExecutionTracer from "./staticstate/ExecutionTracer";
import {TaintType} from "./shared/TaintType";


class StatementDescription {
    readonly line: string;
    readonly method: string;

    constructor(line: string, method: string) {
        this.line = line;
        this.method = method;
    }
}

/**
 * Besides code coverage, there can be additional info that we want
 * to collect at runtime when test cases are executed.
 */
export default class AdditionalInfo {

    /**
     * In REST APIs, it can happen that some query parameters do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
    private queryParameters: Set<string> = new Set<string>();


    /**
     * In REST APIs, it can happen that some HTTP headers do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
    private headers: Set<string> = new Set<string>();

    /**
     * Map from taint input name to string specializations for it
     */
    private stringSpecializations: Map<string, Set<StringSpecializationInfo>> = new Map<string, Set<StringSpecializationInfo>>();


    /*
        WARNING: code below on lastExecutedStatementStack is different from Java version:
        we always force a pop after each statement, and we do not collect info on Methdo
     */

    /**
     * Keep track of the last executed statement done in the SUT.
     * But not in the third-party libraries, just the business logic of the SUT.
     * The statement is represented with a descriptive unique id, like the class name and line number.
     *
     * We need to use a stack to handle method call invocations, as we can know when a statement
     * starts, but not so easily when it ends.
     */
    private lastExecutedStatementStack: Array<string> = new Array<string>();

    /**
     * In case we pop all elements from stack, keep track of last one separately.
     */
    private noExceptionStatement: string = null;


    addSpecialization(taintInputName: string, info: StringSpecializationInfo) {
        if (ExecutionTracer.getTaintType(taintInputName) == TaintType.NONE) {
            throw new Error("No valid input name: " + taintInputName);
        }
        if (!info) {
            throw new Error("Missing info object");
        }

        let set = this.stringSpecializations.get(taintInputName);
        if (!set) {
            set = new Set<StringSpecializationInfo>();
            this.stringSpecializations.set(taintInputName, set);
        }

        /*
            ah! the joys of JS Set when dealing with objects...
         */
        for(let s of set){
            if(info.equalsTo(s)){
                return;
            }
        }

        set.add(info);
    }

    getStringSpecializationsView() :  Map<string, Set<StringSpecializationInfo>> {
        return this.stringSpecializations;
    }

    public addQueryParameter(param: string) {
        if (param && param.length > 0) {
            this.queryParameters.add(param);
        }
    }

    public getQueryParametersView(): Set<string> {
        return this.queryParameters
    }

    public addHeader(header: string) {
        if (header && header.length > 0) {
            this.headers.add(header);
        }
    }

    public getHeadersView(): Set<string> {
        return this.headers;
    }

    public getLastExecutedStatement(): string {

        if (this.lastExecutedStatementStack.length == 0) {
            if (!this.noExceptionStatement) {
                return null;
            }
            return this.noExceptionStatement;
        }

        return this.lastExecutedStatementStack[this.lastExecutedStatementStack.length - 1];
    }

    public pushLastExecutedStatement(lastLine: string) {

        this.noExceptionStatement = null;

        this.lastExecutedStatementStack.push(lastLine);
    }

    public popLastExecutedStatement(): string {
        const statementDescription = this.lastExecutedStatementStack.pop();
        if (this.lastExecutedStatementStack.length == 0) {
            this.noExceptionStatement = statementDescription;
        }
        return statementDescription;
    }
}
