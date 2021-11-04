package org.evomaster.e2etests.utils;

import com.google.inject.Injector;
import kotlin.Unit;
import org.apache.commons.io.FileUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;
import org.evomaster.core.Main;
import org.evomaster.core.StaticCounter;
import org.evomaster.core.logging.TestLoggingUtil;
import org.evomaster.core.output.OutputFormat;
import org.evomaster.core.output.compiler.CompilerForTestGenerated;
import org.evomaster.core.problem.httpws.service.HttpWsIndividual;
import org.evomaster.core.remote.service.RemoteController;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public abstract class WsTestBase {

    protected static InstrumentedSutStarter embeddedStarter;
    protected static String baseUrlOfSut;
    protected static SutController controller;
    protected static RemoteController remoteController;
    protected static int controllerPort;


    private final static int STARTING_SEED = 42;
    protected int defaultSeed = STARTING_SEED;

    public final static String TESTS_OUTPUT_ROOT_FOLDER = "target/em-tests/";


    @AfterAll
    public static void tearDown() {

        assertTimeoutPreemptively(Duration.ofMinutes(2), () -> {
            boolean stopped = remoteController.stopSUT();
            stopped = embeddedStarter.stop() && stopped;

            assertTrue(stopped);
        });

        SimpleLogger.setThreshold(SimpleLogger.Level.INFO);
    }


    @BeforeEach
    public void initTest() {

        //in case it was modified in a previous test in the same class
        defaultSeed = STARTING_SEED;

        StaticCounter.Companion.reset();

        assertTimeoutPreemptively(Duration.ofMinutes(2), () -> {
            boolean reset = remoteController.resetSUT();
            assertTrue(reset);
        });

        SimpleLogger.setThreshold(SimpleLogger.Level.DEBUG);
    }


    protected Injector init(List<String> args) {
        return Main.init(args.toArray(new String[0]));
    }


    protected String outputFolderPath(String outputFolderName){
        return TESTS_OUTPUT_ROOT_FOLDER + outputFolderName;
    }


    protected void runAndCheckDeterminism(int iterations, Consumer<List<String>> lambda){

        List<String> args =  new ArrayList<>(Arrays.asList(
                "--createTests", "false",
                "--seed", "42",
                "--showProgress", "false",
                "--avoidNonDeterministicLogs", "true",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "" + iterations,
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--useTimeInFeedbackSampling" , "false"
        ));

        StaticCounter.Companion.reset();
        String firstRun = TestLoggingUtil.Companion.runWithDeterministicLogger(
                () -> {lambda.accept(args); return Unit.INSTANCE;}
        );

        StaticCounter.Companion.reset();
        String secondRun = TestLoggingUtil.Companion.runWithDeterministicLogger(
                () -> {lambda.accept(args); return Unit.INSTANCE;}
        );

        assertEquals(firstRun, secondRun);
    }

    protected void runTestHandlingFlaky(
            String outputFolderName,
            String fullClassName,
            int iterations,
            boolean createTests,
            Consumer<List<String>> lambda) throws Throwable{

        runTestHandlingFlaky(outputFolderName, fullClassName, iterations, createTests, lambda, 3);
    }


    protected void runTestHandlingFlaky(
            String outputFolderName,
            String fullClassName,
            int iterations,
            boolean createTests,
            Consumer<List<String>> lambda,
            int timeoutMinutes) throws Throwable{

        runTestHandlingFlaky(outputFolderName, fullClassName, null, iterations, createTests, lambda, timeoutMinutes);
    }

    protected void runTestHandlingFlaky(
            String outputFolderName,
            String fullClassName,
            List<String> terminations,
            int iterations,
            boolean createTests,
            Consumer<List<String>> lambda,
            int timeoutMinutes) throws Throwable{

        List<ClassName> classNames = new ArrayList<>();

        String splitType = "";

        if(terminations == null || terminations.isEmpty()){
            classNames.add(new ClassName(fullClassName));
            splitType = "NONE";
        } else {
            for (String termination : terminations) {
                classNames.add(new ClassName(fullClassName + termination));
            }
            splitType = "CLUSTER";
        }

         /*
            Years have passed, still JUnit 5 does not handle global test timeouts :(
            https://github.com/junit-team/junit5/issues/80
         */
        String finalSplitType = splitType;
        assertTimeoutPreemptively(Duration.ofMinutes(timeoutMinutes), () -> {
            ClassName className = new ClassName(fullClassName);
            clearGeneratedFiles(outputFolderName, classNames);

            handleFlaky(
                    () -> {
                        List<String> args = getArgsWithCompilation(iterations, outputFolderName, className, createTests, finalSplitType, "FALSE");
                        defaultSeed++;
                        lambda.accept(new ArrayList<>(args));
                    }
            );
        });
    }



    protected void runTestHandlingFlakyAndCompilation(
            String outputFolderName,
            String fullClassName,
            int iterations,
            Consumer<List<String>> lambda) throws Throwable {

        runTestHandlingFlakyAndCompilation(outputFolderName, fullClassName, null, iterations, true, lambda, 3);
    }

    protected void runTestHandlingFlakyAndCompilation(
            String outputFolderName,
            String fullClassName,
            List<String> terminations,
            int iterations,
            Consumer<List<String>> lambda) throws Throwable {

        runTestHandlingFlakyAndCompilation(outputFolderName, fullClassName, terminations, iterations, true, lambda, 3);
    }

    protected void runTestHandlingFlakyAndCompilation(
            String outputFolderName,
            String fullClassName,
            List<String> terminations,
            int iterations,
            boolean createTests,
            Consumer<List<String>> lambda,
            int timeoutMinutes) throws Throwable {

        runTestHandlingFlaky(outputFolderName, fullClassName, terminations, iterations, createTests,lambda, timeoutMinutes);

        if (terminations == null) terminations = Arrays.asList("");
        //BMR: this is where I should handle multiples???
        if (createTests){
            for (String termination : terminations) {
                assertTimeoutPreemptively(Duration.ofMinutes(2), () -> {
                    ClassName className = new ClassName(fullClassName + termination);
                    clearCompiledFiles(className);
                    //the first one goes through, but for the second generated files appear to not be clean.
                    compileRunAndVerifyTests(outputFolderName, className);
                });
            }
        }
    }

    protected void runTestHandlingFlakyAndCompilation(
            String outputFolderName,
            String fullClassName,
            int iterations,
            boolean createTests,
            Consumer<List<String>> lambda,
            int timeoutMinutes) throws Throwable {

        runTestHandlingFlaky(outputFolderName, fullClassName, iterations, createTests,lambda, timeoutMinutes);

        if (createTests){
            assertTimeoutPreemptively(Duration.ofMinutes(2), () -> {
                ClassName className = new ClassName(fullClassName);
                compileRunAndVerifyTests(outputFolderName, className);
            });
        }
    }

    protected void compileRunAndVerifyTests(String outputFolderName, ClassName className){

        ExecutionTracer.setKillSwitch(false); //make sure it is not on

        Class<?> klass = loadClass(className);
        assertNull(klass);

        compile(outputFolderName);
        klass = loadClass(className);
        assertNotNull(klass);

        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);

        TestExecutionSummary summary = JUnitTestRunner.runTestsInClass(klass);
        summary.printFailuresTo(pw, 100);
        String failures = writer.toString();

        assertTrue(summary.getContainersFoundCount() > 0);
        assertEquals(0, summary.getContainersFailedCount(), failures);
        assertTrue(summary.getContainersSucceededCount() > 0);
        assertTrue(summary.getTestsFoundCount() > 0);
        assertEquals(0, summary.getTestsFailedCount(), failures);
        assertTrue(summary.getTestsSucceededCount() > 0);
    }

    protected void clearGeneratedFiles(String outputFolderName, List<ClassName> testClassNames){

        File folder = new File(outputFolderPath(outputFolderName));
        try{
            FileUtils.deleteDirectory(folder);
        }catch (Exception e){
            throw new RuntimeException(e);
        }

        for (ClassName testClassName : testClassNames){
            clearCompiledFiles(testClassName);
        }

    }

    protected void clearGeneratedFiles(String outputFolderName, ClassName testClassName){
        List<ClassName> classNames = new ArrayList<ClassName>();
        classNames.add(testClassName);

        clearGeneratedFiles(outputFolderName, classNames);
    }

    /**
     *  As E2E generates test cases, we need delete the previous ones from previous runs, to make sure
     *  we are running the latest generated.
     *
     *  However, if you run everything from "org.", those existing tests from previous run will be loaded into
     *  the JVM, and so checks for their presence after this is executed will pass... and so the E2E will fail
     */
    protected void clearCompiledFiles(ClassName testClassName){
        String byteCodePath = "target/test-classes/" + testClassName.getAsResourcePath();
        File compiledFile = new File(byteCodePath);
        boolean result = compiledFile.delete();
    }

    protected Class<?> loadClass(ClassName className){
        try {
            return this.getClass().getClassLoader().loadClass(className.getFullNameWithDots());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    protected void compile(String outputFolderName){

        CompilerForTestGenerated.INSTANCE.compile(
                OutputFormat.KOTLIN_JUNIT_5,
                new File(outputFolderPath(outputFolderName)),
                new File("target/test-classes")
        );
    }

    protected List<String> getArgsWithCompilation(int iterations, String outputFolderName, ClassName testClassName){
        return getArgsWithCompilation(iterations, outputFolderName, testClassName, true);
    }

    protected List<String> getArgsWithCompilation(int iterations, String outputFolderName, ClassName testClassName, boolean createTests){
        return getArgsWithCompilation(iterations, outputFolderName, testClassName, createTests, "NONE", "FALSE");
    }
    protected List<String> getArgsWithCompilation(int iterations, String outputFolderName, ClassName testClassName, boolean createTests, String split, String summary){

        return new ArrayList<>(Arrays.asList(
                "--createTests", "" + createTests,
                "--seed", "" + defaultSeed,
                "--useTimeInFeedbackSampling" , "false",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "" + iterations,
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--outputFolder", outputFolderPath(outputFolderName),
                "--outputFormat", OutputFormat.KOTLIN_JUNIT_5.toString(),
                //FIXME: should avoid deprecated option, but then need TODO update how class files are deleted from FS
                "--testSuiteFileName", testClassName.getFullNameWithDots(),
                "--testSuiteSplitType", split,
                "--expectationsActive", "TRUE",
                "--executiveSummary", summary
        ));
    }


    /**
     * Unfortunately JUnit 5 does not handle flaky tests, and Maven is not upgraded yet.
     * See https://github.com/junit-team/junit5/issues/1558#issuecomment-414701182
     *
     * TODO: once that issue is fixed (if it will ever be fixed), then this method
     * will no longer be needed.
     * Actually no... as we change the seed at each re-execution... so we still need
     * this code
     *
     * @param lambda
     * @throws Throwable
     */
    protected void handleFlaky(Runnable lambda) throws Throwable{

        int attempts = 3;
        Throwable error = null;

        for(int i=0; i<attempts; i++){

            try{
                lambda.run();
                return;
            }catch (OutOfMemoryError e){
                throw e;
            }catch (Throwable t){
                error = t;
            }
        }

        throw error;
    }


    protected static void initClass(EmbeddedSutController controller) throws Exception {

        WsTestBase.controller = controller;

        embeddedStarter = new InstrumentedSutStarter(controller);
        embeddedStarter.start();

        controllerPort = embeddedStarter.getControllerServerPort();

        remoteController = new RemoteController("localhost", controllerPort, true);
        boolean started = remoteController.startSUT();
        assertTrue(started);

        SutInfoDto dto = remoteController.getSutInfo();
        assertNotNull(dto);

        baseUrlOfSut = dto.baseUrlOfSUT;
        assertNotNull(baseUrlOfSut);

        System.out.println("Remote controller running on port " + controllerPort);
        System.out.println("SUT listening on " + baseUrlOfSut);
    }


    protected void assertInsertionIntoTable(Solution<? extends HttpWsIndividual> solution, String tableName) {

        boolean ok = solution.getIndividuals().stream().anyMatch(
                ind -> ind.getIndividual().seeInitializingActions().stream().anyMatch(
                        da -> da.getTable().getName().equalsIgnoreCase(tableName))
        );

        assertTrue(ok);
    }

    /**
     * Bit of a helper method to replace the default output format for testing purposes.
     *
     * @param args - the list of arguments
     * @param outputFormat - the desired output format
     */
    protected void setOutputFormat(List<String> args, OutputFormat outputFormat){
        if (outputFormat != null){
            args.replaceAll(s -> s.replace(OutputFormat.KOTLIN_JUNIT_5.name(), outputFormat.name()));
        }
    }
}
