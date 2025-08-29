package org.evomaster.e2etests.spring.examples.constant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.rest.examples.spring.constant.ConstantController;
import com.foo.rest.examples.spring.constant.ConstantResponseDto;
import org.evomaster.core.problem.rest.data.RestCallResult;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConstantEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new ConstantController());
    }

    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "ConstantEM",
                "org.foo.ConstantEM",
                2000,
                (args) -> {

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    ObjectMapper mapper = new ObjectMapper();

                    //get number of distinct response values
                    List<Boolean> responses = solution.getIndividuals().stream()
                            .flatMap(i -> i.seeResults(null).stream())
                            .map(r -> r.getResultValue(RestCallResult.BODY))
                            .filter(s -> s != null)
                            .map(s -> {
                                try {
                                    return mapper.readValue(s, ConstantResponseDto.class);
                                } catch (IOException e) {
                                    return null;
                                }
                            })
                            .filter(b -> b != null)
                            .map(b -> b.ok)
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList());

                    long n = responses.size();

                    assertEquals(2, n);
                });
    }
}