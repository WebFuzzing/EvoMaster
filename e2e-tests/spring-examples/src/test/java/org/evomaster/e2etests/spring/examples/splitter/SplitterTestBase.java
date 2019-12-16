package org.evomaster.e2etests.spring.examples.splitter;

import com.foo.rest.examples.spring.strings.StringsController;
import org.apache.commons.io.FileUtils;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.evomaster.e2etests.utils.JUnitTestRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SplitterTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new StringsController());
    }

    protected void runTestHandlingFlakyAndCompilation(
            String outputFolderName,
            String fullClassName,
            int iterations,
            Consumer<List<String>> lambda) throws Throwable {

        runTestHandlingFlakyAndCompilation(outputFolderName, fullClassName, Arrays.asList(""), iterations, true, lambda, 3);
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

    protected void runTestHandlingFlaky(
            String outputFolderName,
            String fullClassName,
            List<String> terminations,
            int iterations,
            boolean createTests,
            Consumer<List<String>> lambda,
            int timeoutMinutes) throws Throwable{

        /*
            Years have passed, still JUnit 5 does not handle global test timeouts :(
            https://github.com/junit-team/junit5/issues/80
         */
        //

        List<ClassName> classNames = new ArrayList<ClassName>(Collections.emptyList());

        for (String termination : terminations) {
            classNames.add(new ClassName(fullClassName + termination));
        }


        assertTimeoutPreemptively(Duration.ofMinutes(timeoutMinutes), () -> {
            ClassName className = new ClassName(fullClassName);
            clearGeneratedFiles(outputFolderName, classNames);

            List<String> args = getArgsWithCompilation(iterations, outputFolderName, className, createTests);

            handleFlaky(
                    () -> lambda.accept(new ArrayList<>(args))
            );
        });
    }

    protected void compileRunAndVerifyTests(String outputFolderName, ClassName className){

        Class<?> klass = loadClass(className);
        assertNull(klass);

        compile(outputFolderName);
        klass = loadClass(className);
        assertNotNull(klass);

        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);

        TestExecutionSummary summary = JUnitTestRunner.runTestsInClass(klass);
        summary.printFailuresTo(pw);
        String failures = writer.toString();

        assertTrue(summary.getContainersFoundCount() > 0);
        assertEquals(0, summary.getContainersFailedCount(), failures);
        assertTrue(summary.getContainersSucceededCount() > 0);
        assertTrue(summary.getTestsFoundCount() > 0);
        assertEquals(0, summary.getTestsFailedCount(), failures);
        assertTrue(summary.getTestsSucceededCount() > 0);
    }

    protected void clearGeneratedFiles(String outputFolderName, ClassName testClassName){
        List<ClassName> classNames = new ArrayList<ClassName>();
        classNames.add(testClassName);

        clearGeneratedFiles(outputFolderName, classNames);
    }

    protected void clearCompiledFiles(ClassName testClassName){
        String byteCodePath = "target/test-classes/" + testClassName.getAsResourcePath();
        File compiledFile = new File(byteCodePath);
        boolean result = compiledFile.delete();

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
            /*String bytecodePath = "target/test-classes/" + testClassName.getAsResourcePath();
            File compiledFile = new File(bytecodePath);
            compiledFile.delete();*/
        }

    }
}
