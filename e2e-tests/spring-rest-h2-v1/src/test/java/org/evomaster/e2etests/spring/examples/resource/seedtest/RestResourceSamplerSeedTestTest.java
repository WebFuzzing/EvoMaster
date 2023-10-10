package org.evomaster.e2etests.spring.examples.resource.seedtest;

import com.google.inject.Injector;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.problem.rest.service.ResourceRestMutator;
import org.evomaster.core.problem.rest.service.ResourceSampler;
import org.evomaster.core.problem.rest.service.RestResourceFitness;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.e2etests.spring.examples.resource.ResourceMIOHWTestBase;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RestResourceSamplerSeedTestTest extends ResourceMIOHWTestBase {

    @Test
    public void testResourceSamplerWithSeed() {

        List<String> args = generalArgs(1, 42);
        seedTestConfig(args);

        Injector injector = init(args);

        ResourceSampler sampler = injector.getInstance(ResourceSampler.class);
        assertEquals(12, sampler.getSizeOfAdHocInitialIndividuals());
        assertEquals(1, sampler.numberOfNotExecutedSeededIndividuals());

        sampler.getNotExecutedAdHocInitialIndividuals().forEach(s-> s.getResourceCalls().forEach(r-> assertNotNull(r.getResourceNode())));
        sampler.getNotExecutedSeededIndividuals().forEach(s-> s.getResourceCalls().forEach(r-> assertNotNull(r.getResourceNode())));

    }

    @Test
    public void testResourceSamplerWithoutSeed() {

        List<String> args = generalArgs(1, 42);

        Injector injector = init(args);

        ResourceSampler sampler = injector.getInstance(ResourceSampler.class);
        assertEquals(12, sampler.getSizeOfAdHocInitialIndividuals());
        sampler.getNotExecutedAdHocInitialIndividuals().forEach(s-> s.getResourceCalls().forEach(r-> assertNotNull(r.getResourceNode())));
    }

}
