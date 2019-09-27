package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Besides code coverage, there can be additional info that we want
 * to collect at runtime when test cases are executed.
 */
public class AdditionalInfo implements Serializable {

    /**
     * In REST APIs, it can happen that some query parameters do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
    private Set<String> queryParameters = new CopyOnWriteArraySet<>();


    /**
     * In REST APIs, it can happen that some HTTP headers do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
    private Set<String> headers = new CopyOnWriteArraySet<>();

    /**
     * Map from taint input name to string specializations for it
     */
    private Map<String, Set<StringSpecializationInfo>> stringSpecializations = new ConcurrentHashMap<>();

    private static class StatementDescription implements Serializable{
        public final String line;
        public final String method;

        public StatementDescription(String line, String method) {
            this.line = line;
            this.method = method;
        }
    }

    /**
     * Keep track of the last executed statement done in the SUT.
     * But not in the third-party libraries, just the business logic of the SUT.
     * The statement is represented with a descriptive unique id, like the class name and line number.
     *
     * We need to use a stack to handle method call invocations, as we can know when a statement
     * starts, but not so easily when it ends.
     */
    private Deque<StatementDescription> lastExecutedStatementStack = new ArrayDeque<>();

    /**
     * In case we pop all elements from stack, keep track of last one separately.
     */
    private StatementDescription noExceptionStatement = null;


    public void addSpecialization(String taintInputName, StringSpecializationInfo info){
        if(!ExecutionTracer.getTaintType(taintInputName).isTainted()){
            throw new IllegalArgumentException("No valid input name: " + taintInputName);
        }
        Objects.requireNonNull(info);

        stringSpecializations.putIfAbsent(taintInputName, new CopyOnWriteArraySet<>());
        Set<StringSpecializationInfo> set = stringSpecializations.get(taintInputName);
        set.add(info);
    }

    public Map<String, Set<StringSpecializationInfo>> getStringSpecializationsView(){
        //note: this does not prevent modifying the sets inside it
        return Collections.unmodifiableMap(stringSpecializations);
    }

    public void addQueryParameter(String param){
        if(param != null && ! param.isEmpty()){
            queryParameters.add(param);
        }
    }

    public Set<String> getQueryParametersView(){
        return Collections.unmodifiableSet(queryParameters);
    }

    public void addHeader(String header){
        if(header != null && ! header.isEmpty()){
            headers.add(header);
        }
    }

    public Set<String> getHeadersView(){
        return Collections.unmodifiableSet(headers);
    }

    public String getLastExecutedStatement() {

        if(lastExecutedStatementStack.isEmpty()){
            if(noExceptionStatement == null){
                return null;
            }
            return noExceptionStatement.line;
        }

        StatementDescription current = lastExecutedStatementStack.peek();
        return current.line;
    }

    public void pushLastExecutedStatement(String lastLine, String lastMethod) {

        noExceptionStatement = null;

        StatementDescription statement = new StatementDescription(lastLine, lastMethod);
        StatementDescription current = lastExecutedStatementStack.peek();

        //if some method, then replace top of stack
        if(current != null && lastMethod.equals(current.method)){
            lastExecutedStatementStack.pop();
        }

        lastExecutedStatementStack.push(statement);
    }

    public void popLastExecutedStatement(){
        StatementDescription statementDescription = lastExecutedStatementStack.pop();
        if(lastExecutedStatementStack.isEmpty()){
            noExceptionStatement = statementDescription;
        }
    }
}
