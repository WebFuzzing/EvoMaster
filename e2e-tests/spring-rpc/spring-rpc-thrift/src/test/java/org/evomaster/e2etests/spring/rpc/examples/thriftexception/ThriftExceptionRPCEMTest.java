package org.evomaster.e2etests.spring.rpc.examples.thriftexception;

import com.foo.rpc.examples.spring.thriftexception.ThriftExceptionRPCController;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThriftExceptionRPCEMTest extends SpringRPCTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        ThriftExceptionRPCController controller = new ThriftExceptionRPCController();
        SpringRPCTestBase.initClass(controller);
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "ThriftExceptionRPCEM",
                "org.foo.ThriftExceptionRPCEM",
                Arrays.asList("_exceptions","_others"),
                5000,
                (args) -> {

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertResponseContainCustomizedException(solution, "com.foo.rpc.examples.spring.thriftexception.BadResponse","bad response: foo");
                    assertResponseContainCustomizedException(solution, "com.foo.rpc.examples.spring.thriftexception.ErrorResponse","error response: empty");
                    assertResponseContainException(solution, "APP_INTERNAL_ERROR");
                });

        // two files for exception and others
        Path exceptionPath = Paths.get("target/em-tests/ThriftExceptionRPCEM/org/foo/ThriftExceptionRPCEM_exceptions.kt");
        assertTrue(Files.exists(exceptionPath));
        Path otherPath = Paths.get("target/em-tests/ThriftExceptionRPCEM/org/foo/ThriftExceptionRPCEM_others.kt");
        assertTrue(Files.exists(otherPath));
    }
}
