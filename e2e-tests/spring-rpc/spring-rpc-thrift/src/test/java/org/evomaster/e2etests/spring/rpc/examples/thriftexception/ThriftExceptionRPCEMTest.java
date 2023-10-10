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
                Arrays.asList("_P0_exceptions","_P1_exceptions","_ThriftExceptionService_Iface_others"),
                5000,
                (args) -> {

                    Solution<RPCIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    assertResponseContainCustomizedException(solution, "com.foo.rpc.examples.spring.thriftexception.BadResponse","bad response: foo");
                    assertResponseContainCustomizedException(solution, "com.foo.rpc.examples.spring.thriftexception.ErrorResponse","error response: empty");
                    assertResponseContainException(solution, "APP_INTERNAL_ERROR");
                });

        // three files for exception and others
        Path exceptionPath = Paths.get("target/em-tests/ThriftExceptionRPCEM/org/foo/ThriftExceptionRPCEM_P0_exceptions.kt");
        assertTrue(Files.exists(exceptionPath));
        exceptionPath = Paths.get("target/em-tests/ThriftExceptionRPCEM/org/foo/ThriftExceptionRPCEM_P1_exceptions.kt");
        assertTrue(Files.exists(exceptionPath));
        Path otherPath = Paths.get("target/em-tests/ThriftExceptionRPCEM/org/foo/ThriftExceptionRPCEM_ThriftExceptionService_Iface_others.kt");
        assertTrue(Files.exists(otherPath));
    }
}
