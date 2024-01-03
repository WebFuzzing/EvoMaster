package org.evomaster.client.java.instrumentation.staticstate;

import org.evomaster.client.java.instrumentation.*;
import org.evomaster.client.java.instrumentation.heuristic.HeuristicsForJumps;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.instrumentation.shared.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Methods of this class will be injected in the SUT to
 * keep track of what the tests do execute/cover.
 * <p>
 * A problem here is that we can have threads left executing SUT instrumented classes after a HTTP call is finished,
 * which can lead to an inconsistent state.
 * So, we added thread synchronization.
 * But issue of possible performance overhead, as this is done on every single instruction... :(
 * Furthermore, this issue does not really seem to happen in Spring... as the sending of HTTP responses is not handled
 * in the instrumented classes, but rather in the framework itself.
 * <p>
 * TODO sync only when necessary... however, currently the overhead seems minimal (but proper tests would be
 * needed to confirm it)
 */
public class ExecutionTracer {

    /**
     * indicate whether now it is to execute sql initialized by evomaster
     */
    private static boolean executingInitSql = false;

    private static boolean executingInitMongo = false;

    /**
     * indicate whether now it is to execute action during the search
     */
    private static boolean executingAction = false;

    /*
        Careful if you change the signature of any of the
        methods in this class, as they are injected in the
        bytecode instrumentation.
        Fortunately, unit tests should quickly find such
        type of issues.
     */


    /**
     * Key -> the unique descriptive id of the coverage objective
     */
    private static final Map<String, TargetInfo> objectiveCoverage =
            new ConcurrentHashMap<>(65536);

    /**
     * A test case can be composed by 1 or more actions, eg HTTP calls.
     * When we get the best distance for a testing target, we might
     * also want to know which action in the test led to it.
     */
    private static int actionIndex = 0;


    private static String actionName = null;

    /**
     * A set of possible values used in the tests, needed for some kinds
     * of taint analyses
     */
    private static Set<String> inputVariables = new HashSet<>();

    /**
     * Map of external services mapping. Key is the WireMock signature
     * (use protocol, remote hostname, and port), and value contains information
     * about remote hostname, mock server local IP address, state
     * (active or inactive) and WireMock signature.
     */
    private static Map<String, ExternalServiceMapping> externalServiceMapping = new HashMap<>();

    private static Map<String, String> localAddressMapping = new HashMap<>();

    /**
     * Besides code coverage, there might be other events that we want to
     * keep track during test execution.
     * We keep track of it separately for each action
     */
    private static final List<AdditionalInfo> additionalInfoList = new ArrayList<>();

    /**
     * Keep track of threads left sleeping, as we will need to interrupt them
     * to avoid issues (eg, out of resources and instrumentation collection when threads wake up
     * during a different action)
     */
    private static final List<Thread> sleepingThreads = new CopyOnWriteArrayList<>();

    /**
     * Keep track of expensive operations. Might want to skip doing them if too many.
     * This should be re-set for each action
     */
    private static int expensiveOperation = 0;

    private static final Object lock = new Object();

    private static List<ExternalService> skippedExternalServices = new CopyOnWriteArrayList<>();
    /**
     * One problem is that, once a test case is evaluated, some background tests might still be running.
     * We want to kill them to avoid issue (eg, when evaluating new tests while previous threads
     * are still running).
     */
    private static volatile boolean killSwitch = false;

    /**
     * When executing method replacements, keep track here of the caller class, ie the class name in which
     * the method replacement took place.
     */
    private static volatile String lastCallerClass = null;

    static {
        reset();
    }


    public static void reset() {
        synchronized (lock) {
            objectiveCoverage.clear();
            actionIndex = 0;
            actionName = null;
            additionalInfoList.clear();
            additionalInfoList.add(new AdditionalInfo());
            inputVariables = new HashSet<>();
            killSwitch = false;
            expensiveOperation = 0;
            executingAction = false;
            sleepingThreads.clear();
            lastCallerClass = null;
            skippedExternalServices = new ArrayList<>();
        }
    }


    public static String getActionName() {
        return actionName;
    }


    public static final String SET_LAST_CALLER_CLASS_METHOD_NAME = "setLastCallerClass";

    public static final String SET_LAST_CALLER_CLASS_DESC = "(Ljava/lang/String;)V";

    public static void setLastCallerClass(String className) {
        lastCallerClass = ClassName.get(className).getFullNameWithDots();
    }

    public static ClassLoader getLastCallerClassLoader() {
        return UnitsInfoRecorder.getInstance().getClassLoaders(getLastCallerClass()).get(0);
    }

    public static String getLastCallerClass() {
        return lastCallerClass;
    }

    public static void reportSleeping() {
        sleepingThreads.add(Thread.currentThread());
    }

    public static boolean isKillSwitch() {
        return killSwitch;
    }

    public static void setKillSwitch(boolean killSwitch) {
        ExecutionTracer.killSwitch = killSwitch;
        if (killSwitch) {
            synchronized (lock) {
                for (Thread t : sleepingThreads) {
                    if (t.isAlive()) {
                        t.interrupt();
                    }
                }
                sleepingThreads.clear();
            }
        }
    }

    public static boolean isExecutingInitSql() {
        return executingInitSql;
    }

    public static void setExecutingInitSql(boolean executingInitSql) {
        ExecutionTracer.executingInitSql = executingInitSql;
    }

    public static void setExecutingInitMongo(boolean executingInitMongo) {
        ExecutionTracer.executingInitMongo = executingInitMongo;
    }

    public static boolean isExecutingAction() {
        return executingAction;
    }


    public static void setExecutingAction(boolean executingAction) {
        ExecutionTracer.executingAction = executingAction;
    }

    public static void setAction(Action action) {
        synchronized (lock) {
            setKillSwitch(false);
            expensiveOperation = 0;

            if (action.getIndex() != actionIndex) {
                actionIndex = action.getIndex();
                additionalInfoList.add(new AdditionalInfo());
            }

            actionName = action.getName();

            if (action.getInputVariables() != null && !action.getInputVariables().isEmpty()) {
                inputVariables = action.getInputVariables();
            }

            if (action.getIndex() == 0) {
                externalServiceMapping = action.getExternalServiceMapping();
                localAddressMapping = action.getLocalAddressMapping();
                skippedExternalServices = action.getSkippedExternalServices();
            }
        }
    }

    public static void increaseExpensiveOperationCount() {
        expensiveOperation++;
    }

    public static boolean isTooManyExpensiveOperations() {
        return expensiveOperation >= 50;
    }

    /**
     * Check if the given input represented a tainted value from the test cases.
     * This could be based on static info of the input (eg, according to a precise
     * name convention given by TaintInputName), or dynamic info given directly by
     * the test itself (eg, the test at action can register a list of values to check
     * for)
     */
    public static boolean isTaintInput(String input) {
        return TaintInputName.isTaintInput(input) || inputVariables.contains(input);
    }

    public static void handleExtraParamTaint(String left, String right) {

        if (left == null || left.isEmpty() || right == null || right.isEmpty()) {
            //nothing to do?
            return;
        }

        boolean taintedLeft = left.equals(TaintInputName.EXTRA_PARAM_TAINT);
        boolean taintedRight = right.equals(TaintInputName.EXTRA_PARAM_TAINT);

        if (taintedLeft && taintedRight) {
            //nothing to do?
            return;
        }

        if (taintedLeft) {
            ExecutionTracer.addQueryParameter(right);
        }
        if (taintedRight) {
            ExecutionTracer.addQueryParameter(left);
        }
    }

    public static void handleExtraHeaderTaint(String left, String right) {

        if (left == null || left.isEmpty() || right == null || right.isEmpty()) {
            //nothing to do?
            return;
        }

        boolean taintedLeft = left.equals(TaintInputName.EXTRA_HEADER_TAINT);
        boolean taintedRight = right.equals(TaintInputName.EXTRA_HEADER_TAINT);

        if (taintedLeft && taintedRight) {
            //nothing to do?
            return;
        }

        if (taintedLeft) {
            ExecutionTracer.addHeader(right);
        }
        if (taintedRight) {
            ExecutionTracer.addHeader(left);
        }
    }

    public static void handleTaintForStringEquals(String left, String right, boolean ignoreCase) {

        if (left == null || right == null) {
            //nothing to do?
            return;
        }

        boolean taintedLeft = isTaintInput(left);
        boolean taintedRight = isTaintInput(right);

        if (taintedLeft && taintedRight) {
            if (ignoreCase ? left.equalsIgnoreCase(right) : left.equals(right)) {
                //tainted, but compared to itself. so shouldn't matter
                return;
            }

            /*
                We consider binding only for base versions of taint, ie we ignore
                the special strings provided by the Core, as it would lead to nasty
                side-effects
             */
            if (!TaintInputName.isTaintInput(left) || !TaintInputName.isTaintInput(right)) {
                return;
            }

            if (shouldSkipTaint()) {
                return;
            }

            //TODO could have EQUAL_IGNORE_CASE
            String id = left + "___" + right;
            addStringSpecialization(left, new StringSpecializationInfo(StringSpecialization.EQUAL, id));
            addStringSpecialization(right, new StringSpecializationInfo(StringSpecialization.EQUAL, id));
            return;
        }

        StringSpecialization type = ignoreCase ? StringSpecialization.CONSTANT_IGNORE_CASE
                : StringSpecialization.CONSTANT;

        if (taintedLeft || taintedRight) {

            if (shouldSkipTaint()) {
                return;
            }

            if (taintedLeft) {
                addStringSpecialization(left, new StringSpecializationInfo(type, right));
            } else {
                addStringSpecialization(right, new StringSpecializationInfo(type, left));
            }
        }
    }

    private static boolean shouldSkipTaint() {
        /*
            Very tricky... H2 can cache some results. When executing queries, it can check inputs in previous
            queries to see if differences in results. but those could be tainted values... which mess up
            the taint analysis :( so, as a special case, we need to skip it
         */
//        return Arrays.stream(Thread.currentThread().getStackTrace())
//                .anyMatch(it -> it.getMethodName().equals("sameResultAsLast")
//                        && it.getClassName().equals("org.h2.command.query.Query"));

        /*
            FIXME This does not really work right now, as SQL commands are put on a buffer and analyzed later.
            Such analysis should be done somehow when SQL commands are intercepted, but it is quite a bit of refactoring
            to pass around such info
         */
        return false;
    }

    public static TaintType getTaintType(String input) {

        if (input == null) {
            return TaintType.NONE;
        }

        if (isTaintInput(input)) {
            return TaintType.FULL_MATCH;
        }

        if (TaintInputName.includesTaintInput(input)
                || inputVariables.stream().anyMatch(v -> input.contains(v))) {
            return TaintType.PARTIAL_MATCH;
        }

        return TaintType.NONE;
    }

    public static List<AdditionalInfo> exposeAdditionalInfoList() {
        return additionalInfoList;
    }


    private static AdditionalInfo getCurrentAdditionalInfo() {
        synchronized (lock) {
            return additionalInfoList.get(actionIndex);
        }
    }

    public static void markRawAccessOfHttpBodyPayload() {
        getCurrentAdditionalInfo().setRawAccessOfHttpBodyPayload(true);
    }

    public static void addParsedDtoName(String name) {
        getCurrentAdditionalInfo().addParsedDtoName(name);
    }

    public static void addQueryParameter(String param) {
        getCurrentAdditionalInfo().addQueryParameter(param);
    }

    public static void addHeader(String header) {
        getCurrentAdditionalInfo().addHeader(header);
    }

    public static void addStringSpecialization(String taintInputName, StringSpecializationInfo info) {
        getCurrentAdditionalInfo().addSpecialization(taintInputName, info);
    }

    public static void addSqlInfo(SqlInfo info) {
        if (!executingInitSql)
            getCurrentAdditionalInfo().addSqlInfo(info);
    }

    public static void addMongoInfo(MongoInfo info){
        if (!executingInitMongo)
            getCurrentAdditionalInfo().addMongoInfo(info);
    }

    public static void addMongoCollectionInfo(MongoCollectionInfo info){
        if (!executingInitMongo)
            getCurrentAdditionalInfo().addMongoCollectionInfo(info);
    }

    public static void markLastExecutedStatement(String lastLine, String lastMethod) {
        getCurrentAdditionalInfo().pushLastExecutedStatement(lastLine, lastMethod);
    }

    public static String getLastExecutedStatement() {
        return getCurrentAdditionalInfo().getLastExecutedStatement();
    }

    public static final String COMPLETED_LAST_EXECUTED_STATEMENT_NAME = "completedLastExecutedStatement";
    public static final String COMPLETED_LAST_EXECUTED_STATEMENT_DESCRIPTOR = "()V";

    public static void completedLastExecutedStatement() {
        getCurrentAdditionalInfo().popLastExecutedStatement();
    }

    public static Map<String, TargetInfo> getInternalReferenceToObjectiveCoverage() {
        return objectiveCoverage;
    }

    /**
     * @return the number of objectives that have been encountered
     * during the test execution
     */
    public static int getNumberOfObjectives() {
        return objectiveCoverage.size();
    }

    public static int getNumberOfObjectives(String prefix) {
        return (int) objectiveCoverage
                .entrySet().stream()
                .filter(e -> prefix == null || e.getKey().startsWith(prefix))
                .count();
    }

    /**
     * Note: only the objectives encountered so far can have
     * been recorded. So, this is a relative value, not based
     * on the code of the whole SUT (just the parts executed so far).
     * Therefore, it is quite useless for binary values (ie 0 or 1),
     * like current implementation of basic line coverage.
     *
     * @param prefix used for string matching of which objectives types
     *               to consider, eg only lines or only branches.
     *               Use "" or {@code null} to pick up everything
     * @return
     */
    public static int getNumberOfNonCoveredObjectives(String prefix) {

        return getNonCoveredObjectives(prefix).size();
    }

    public static Set<String> getNonCoveredObjectives(String prefix) {

        return objectiveCoverage
                .entrySet().stream()
                .filter(e -> prefix == null || e.getKey().startsWith(prefix))
                .filter(e -> e.getValue().value < 1)
                .map(e -> e.getKey())
                .collect(Collectors.toSet());
    }

    public static Double getValue(String id) {
        return objectiveCoverage.get(id).value;
    }

    private static void updateObjective(String id, double value) {
        if (value < 0d || value > 1d) {
            throw new IllegalArgumentException("Invalid value " + value + " out of range [0,1]");
        }

        /*
            In the same execution, a target could be reached several times,
            so we should keep track of the best value found so far
         */
        synchronized (lock) {
            if (objectiveCoverage.containsKey(id)) {
                double previous = objectiveCoverage.get(id).value;
                if (value > previous) {
                    objectiveCoverage.put(id, new TargetInfo(null, id, value, actionIndex));
                }
            } else {
                objectiveCoverage.put(id, new TargetInfo(null, id, value, actionIndex));
            }
        }

        ObjectiveRecorder.update(id, value, !executingAction);
    }

    public static void executedNumericComparison(String idTemplate, double lt, double eq, double gt) {

        updateObjective(ObjectiveNaming.numericComparisonObjectiveName(idTemplate, -1), lt);
        updateObjective(ObjectiveNaming.numericComparisonObjectiveName(idTemplate, 0), eq);
        updateObjective(ObjectiveNaming.numericComparisonObjectiveName(idTemplate, +1), gt);
    }

    public static void executedReplacedMethod(String idTemplate, ReplacementType type, Truthness t) {

        /*
            Considering the fact that the method has been executed, and so reached, cannot happen
            that any of the heuristic values is 0
         */
        assert t.getOfTrue() != 0 && t.getOfFalse() != 0;

        String idTrue = ObjectiveNaming.methodReplacementObjectiveName(idTemplate, true, type);
        String idFalse = ObjectiveNaming.methodReplacementObjectiveName(idTemplate, false, type);

        updateObjective(idTrue, t.getOfTrue());
        updateObjective(idFalse, t.getOfFalse());
    }


    public static final String EXECUTED_LINE_METHOD_NAME = "executedLine";
    public static final String EXECUTED_LINE_DESCRIPTOR = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V";

    /**
     * Report on the fact that a given line has been executed.
     */
    public static void executedLine(String className, String methodName, String descriptor, int line) {

        /*
            This is done to prevent the SUT keep on executing code after a test case is evaluated
         */
        if (isKillSwitch()) {

            /*
                This is tricky... not only if we are in the middle of a class initializer (e.g., a
                static block, or static fields initialization), but also if any ancestor calls are
                a class initializer (in bytecode these are marked as <clinit>).
                For example, assume class X has a static intiliazer "static{ Foo f == new Foo()},
                then, if the kill switch is on while we are executing "Foo()", we must not kill the
                execution, otherwise the class initializer of X would fail, and X would not be usable
                anymore inside the whole SUT.
                To check those cases, we look at the stack trace of the method calls.
             */
            boolean initClass = Arrays.stream(Thread.currentThread().getStackTrace())
                    .anyMatch(e -> e.getMethodName().equals("<clinit>"));

            /*
                must NOT stop the initialization of a class, otherwise the SUT will be left in an
                inconsistent state in the following calls
             */

            if (!initClass) {
                throw new KillSwitchException();
            }
        }

        //for targets to cover
        String lineId = ObjectiveNaming.lineObjectiveName(className, line);
        String classId = ObjectiveNaming.classObjectiveName(className);
        updateObjective(lineId, 1d);
        updateObjective(classId, 1d);

        //to calculate last executed line
        String lastLine = className + "_" + line + "_" + methodName;
        String lastMethod = className + "_" + methodName + "_" + descriptor;
        markLastExecutedStatement(lastLine, lastMethod);
    }

    public static final String EXECUTING_METHOD_METHOD_NAME = "executingMethod";
    public static final String EXECUTING_METHOD_DESCRIPTOR = "(Ljava/lang/String;IIZ)V";

    /**
     * Report on whether method calls have been successfully completed.
     * Failures can happen due to thrown exceptions.
     *
     * @param className
     * @param line
     * @param index     as there can be many method calls on same line, need to differentiate them
     * @param completed whether the method call was successfully completed.
     */
    public static void executingMethod(String className, int line, int index, boolean completed) {
        String id = ObjectiveNaming.successCallObjectiveName(className, line, index);
        if (completed) {
            updateObjective(id, 1d);
        } else {
            updateObjective(id, 0.5);
        }
    }


    //---- branch-jump methods --------------------------

    private static void updateBranch(String className, int line, int branchId, Truthness t) {

        /*
            Note: when we have
            if(x > 0){}

            the "jump" to "else" branch is done if that is false.
            So, the actual evaluated condition is the negation, ie
            x <= 0
         */

        String forThen = ObjectiveNaming.branchObjectiveName(className, line, branchId, true);
        String forElse = ObjectiveNaming.branchObjectiveName(className, line, branchId, false);

        updateObjective(forElse, t.getOfTrue());
        updateObjective(forThen, t.getOfFalse());
    }

    public static final String EXECUTING_BRANCH_JUMP_METHOD_NAME = "executingBranchJump";


    public static final String JUMP_DESC_1_VALUE = "(IILjava/lang/String;II)V";

    public static void executingBranchJump(
            int value, int opcode, String className, int line, int branchId) {

        Truthness t = HeuristicsForJumps.getForSingleValueJump(value, opcode);

        updateBranch(className, line, branchId, t);
    }


    public static final String JUMP_DESC_2_VALUES = "(IIILjava/lang/String;II)V";

    public static void executingBranchJump(
            int firstValue, int secondValue, int opcode, String className, int line, int branchId) {

        Truthness t = HeuristicsForJumps.getForValueComparison(firstValue, secondValue, opcode);

        updateBranch(className, line, branchId, t);
    }

    public static final String JUMP_DESC_OBJECTS =
            "(Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/String;II)V";

    public static void executingBranchJump(
            Object first, Object second, int opcode, String className, int line, int branchId) {

        Truthness t = HeuristicsForJumps.getForObjectComparison(first, second, opcode);

        updateBranch(className, line, branchId, t);
    }


    public static final String JUMP_DESC_NULL =
            "(Ljava/lang/Object;ILjava/lang/String;II)V";

    public static void executingBranchJump(
            Object obj, int opcode, String className, int line, int branchId) {

        Truthness t = HeuristicsForJumps.getForNullComparison(obj, opcode);

        updateBranch(className, line, branchId, t);
    }

    public static void addHostnameInfo(HostnameResolutionInfo hostnameResolutionInfo) {
        getCurrentAdditionalInfo().addHostnameInfo(hostnameResolutionInfo);
    }

    /**
     * Add the external HTTP/S hostname to the additional info to keep track.
     */
    public static void addExternalServiceHost(ExternalServiceInfo hostInfo) {
        getCurrentAdditionalInfo().addExternalService(hostInfo);
        // here, we only register external info into ObjectiveRecorder if it occurs during the startup
        if (!executingAction)
            ObjectiveRecorder.registerExternalServiceInfoAtSutStartupTime(hostInfo);
    }

    /**
     * track what host uses the default WM
     */
    public static void addEmployedDefaultWMHost(ExternalServiceInfo hostInfo) {
        getCurrentAdditionalInfo().addEmployedDefaultWM(hostInfo);
    }

    /**
     * Return the WireMock IP if there is a mapping for the hostname. If there is
     * no mapping NULL will be returned
     */
    public static String getExternalMappingForSignature(String signature) {
        if (externalServiceMapping.containsKey(signature)) {
            return externalServiceMapping.get(signature).getLocalIPAddress();
        }
        return null;
    }

    public static String getExternalMappingForHostname(String hostname) {
        return externalServiceMapping
                .entrySet().stream()
                .filter(e -> e.getValue().getRemoteHostname().equals(hostname))
                .findFirst().get().getValue().getLocalIPAddress();
    }

    public static boolean hasActiveExternalMappingForSignature(String signature) {
        return externalServiceMapping.containsKey(signature);
    }

    public static boolean hasMockServerForHostname(String hostname) {
        return externalServiceMapping
                .entrySet().stream().anyMatch(e -> e.getValue().getLocalIPAddress().equals(hostname));
    }

    /**
     * Check whether there is a local IP address available for the given
     * remote hostname.
     */
    public static boolean hasLocalAddressForHost(String hostname) {
        return localAddressMapping.containsKey(hostname);
    }

    /**
     * Checks for any replacement available to given local IP address.
     */
    public static boolean hasMappingForLocalAddress(String localAddress) {
        return localAddressMapping.containsValue(localAddress);
    }

    /**
     * Return the respective remote hostname for the given local IP address
     */
    public static String getRemoteHostname(String localAddress) {
        return localAddressMapping.entrySet()
                .stream()
                .filter(e -> e.getValue().equals(localAddress))
                .map(Map.Entry::getKey)
                .findFirst().get().toString();
    }

    public static String getLocalAddress(String hostname) {
        return localAddressMapping.get(hostname);
    }

    public static boolean skipHostname(String hostname) {
        return skippedExternalServices
                .stream().anyMatch(e -> e.getHostname().equals(hostname.toLowerCase()));
    }

    public static boolean skipHostnameAndPort(String hostname, int port) {
        return skippedExternalServices
                .stream().anyMatch(e -> e.getHostname().equals(hostname.toLowerCase()) && e.getPort() == port);
    }

}
