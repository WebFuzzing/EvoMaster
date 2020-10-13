package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Set<String> queryParameters = new CopyOnWriteArraySet<>();


    /**
     * In REST APIs, it can happen that some HTTP headers do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
    private final Set<String> headers = new CopyOnWriteArraySet<>();

    /**
     * Map from taint input name to string specializations for it
     */
    private final Map<String, Set<StringSpecializationInfo>> stringSpecializations = new ConcurrentHashMap<>();

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
     * For example:
     * foo(bar(), x.npe)
     * here, if x is null, we would end up wrongly marking the last line in bar() as last-statement,
     * whereas it should be the one for foo()
     *
     * Furthermore, we need a stack per execution thread, based on their name.
     */
    private final Map<String, Deque<StatementDescription>> lastExecutedStatementStacks = new ConcurrentHashMap<>();

    /**
     * In case we pop all elements from stack, keep track of last one separately.
     */
    private StatementDescription noExceptionStatement = null;


    /**
     * Check if the business logic of the SUT (and not a third-party library) is
     * accessing the raw bytes of HTTP body payload (if any) directly
     */
    private boolean rawAccessOfHttpBodyPayload = false;


    /**
     * The name of all DTO that have been parsed (eg, with GSON and Jackson).
     * Note: the actual content of schema is queried separately.
     * Reasons: does not change (DTO classes are static), and quite expensive
     * to send at each action evaluation
     */
    private final Set<String> parsedDtoNames = new CopyOnWriteArraySet<>();

    private String lastExecutingThread = null;

    public Set<String> getParsedDtoNamesView(){
        return Collections.unmodifiableSet(parsedDtoNames);
    }

    public void addParsedDtoName(String name){
        parsedDtoNames.add(name);
    }

    public boolean isRawAccessOfHttpBodyPayload() {
        return rawAccessOfHttpBodyPayload;
    }

    public void setRawAccessOfHttpBodyPayload(boolean rawAccessOfHttpBodyPayload) {
        this.rawAccessOfHttpBodyPayload = rawAccessOfHttpBodyPayload;
    }

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

//        if(lastExecutedStatementStacks.values().stream().allMatch(s -> s.isEmpty())){
        /*
            TODO: not super-sure about this... we could have several threads in theory, but hard to
            really say if the last one executing a statement of the SUT is always the one we are really
            interested into... would need to check if there are cases in which this is not the case
         */

        if(lastExecutingThread == null || lastExecutedStatementStacks.get(lastExecutingThread).isEmpty()){
            if(noExceptionStatement == null){
                return null;
            }
            return noExceptionStatement.line;
        }

        Deque<StatementDescription> stack = lastExecutedStatementStacks.get(lastExecutingThread);
        StatementDescription current = stack.peek(); //WARNING this is not thread-safe... but hopefully should not be a problem
        return current.line;
    }

    public void pushLastExecutedStatement(String lastLine, String lastMethod) {

        String key = getThreadIdentifier();
        lastExecutingThread = key;
        lastExecutedStatementStacks.putIfAbsent(key, new ArrayDeque<>());
        Deque<StatementDescription> stack = lastExecutedStatementStacks.get(key);

        noExceptionStatement = null;

        StatementDescription statement = new StatementDescription(lastLine, lastMethod);
        StatementDescription current = stack.peek();

        //if some method, then replace top of stack
        if(current != null && lastMethod.equals(current.method)){
            stack.pop();
        }

        stack.push(statement);
    }

    private String getThreadIdentifier() {
        return "" + Thread.currentThread().getId();
    }

    public void popLastExecutedStatement(){

        String key = getThreadIdentifier();
        Deque<StatementDescription> stack = lastExecutedStatementStacks.get(key);

        if(stack == null || stack.isEmpty()){
            throw new IllegalStateException("[ERROR] EvoMaster: invalid stack pop on thread " + key);
        }

        StatementDescription statementDescription = stack.pop();

        if(stack.isEmpty()){
            noExceptionStatement = statementDescription;
        }
    }
}
