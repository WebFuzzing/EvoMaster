package org.evomaster.e2etests.spring.examples.db.directintwithsql;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.evomaster.core.EMConfig;
import org.evomaster.core.Main;
import org.evomaster.core.sql.SqlAction;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestCallAction;
import org.evomaster.core.problem.rest.data.RestCallResult;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.problem.rest.service.sampler.ResourceSampler;
import org.evomaster.core.remote.service.RemoteController;
import org.evomaster.core.search.action.EvaluatedAction;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.FitnessValue;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.gene.numeric.IntegerGene;
import org.evomaster.core.search.service.FitnessFunction;
import org.evomaster.ci.utils.CIUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DbDirectIntWithSqlEMTest extends DbDirectIntWithSqlTestBase {

    @Test
    public void testDeterminism(){

        runAndCheckDeterminism(100, (args) -> {
            initAndRun(args);
        });

//        runAndCheckDeterminism(2_000, (args) -> {
//            Solution<RestIndividual> solution = initAndRun(args);
//
//            assertTrue(solution.getIndividuals().size() >= 1);
//
//            //the POST is deactivated in the controller
//            assertNone(solution, HttpVerb.POST, 200);
//
//            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/directint/{x}/{y}", null);
//            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/directint/{x}/{y}", null);
//            assertInsertionIntoTable(solution, "DB_DIRECT_INT_ENTITY");
//        });
    }

    /*
        In the SUT, there are 2 endpoints:
        1) a POST that generates some data into the DB
        2) a GET that search for such data, specified by parameters

        To cover all statements, we first need to create data with a POST,
        and then search for it with a GET.

        However, in contrast to DbDirectInt, here we disable the call to POST.
        This is done in the EM Driver.
        This makes the SUT "read-only".
        Therefore, the only way to have the right data in the DB for the GET to
        find it, is to write such data directly with SQL commands.
     */

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testRunEM(boolean heuristicsForSQLAdvanced) throws Throwable {


        runTestHandlingFlakyAndCompilation(
                "DbDirectWithSqlEM",
                "org.bar.db.DirectWithSqlEM"  + (heuristicsForSQLAdvanced ? "Complete" : "Partial"),
                2_000,
                (args) -> {

                    setOption(args, "heuristicsForSQL", "true");
                    setOption(args, "generateSqlDataWithSearch", "true");
                    setOption(args, "heuristicsForSQLAdvanced", heuristicsForSQLAdvanced ? "true" : "false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    assertTrue(solution.getIndividuals().size() >= 1);

                    //the POST is deactivated in the controller
                    assertNone(solution, HttpVerb.POST, 200);

                    assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/db/directint/{x}/{y}", null);
                    assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/db/directint/{x}/{y}", null);
                    assertInsertionIntoTable(solution, "DB_DIRECT_INT_ENTITY");
                });

    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testSteps(boolean heuristicsForSQLAdvanced) {

        CIUtils.skipIfOnCircleCI();

        List<String> args = new ArrayList<>();
        setOption(args, "createTests", "true");
        setOption(args, "seed", "42" );
        setOption(args, "sutControllerPort", "" + controllerPort);
        setOption(args, "maxEvaluations", "1");
        setOption(args, "stoppingCriterion", "ACTION_EVALUATIONS");
        setOption(args, "heuristicsForSQL", "true");
        setOption(args, "generateSqlDataWithSearch", "true");
        setOption(args, "maxTestSize", "1");
        setOption(args, "useTimeInFeedbackSampling" , "false");
        setOption(args, "heuristicsForSQLAdvanced", heuristicsForSQLAdvanced ? "true" : "false");

        Injector injector = Main.init(args.toArray(new String[]{}));

        RemoteController rc = injector.getInstance(RemoteController.class);
        rc.startANewSearch();

        /*
            start from creating and evaluating a random individual
            default REST setting is changed to resource-based solution,
            thus RestSampler needs to be changed to ResourceSampler
         */
        ResourceSampler sampler = injector.getInstance(ResourceSampler.class);
        RestIndividual ind = sampler.sample(true);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));
        EvaluatedIndividual ei = ff.calculateCoverage(ind, Collections.emptySet(), null);
        assertNotNull(ei);

        FitnessValue noDataFV = ei.getFitness();

        assertNotEquals(0, noDataFV.averageExtraDistancesToMinimize(0));

        RestCallResult result = (RestCallResult) ((EvaluatedAction) ei.evaluatedMainActions().get(0)).getResult();
        assertEquals(400, result.getStatusCode().intValue());


        //now, try to execute an action in which as well we add SQL data

        List<SqlAction> insertions = sampler.sampleSqlInsertion("DB_DIRECT_INT_ENTITY", Collections.singleton("*"));
        assertEquals(1, insertions.size());

        //extract the x/y values from the random call
        RestCallAction first = (RestCallAction) ind.seeAllActions().iterator().next();
        int x = first.getParameters().stream()
                .filter(p -> p.getName().equalsIgnoreCase("x"))
                .map(p -> Integer.parseInt(p.getGene().getValueAsRawString()))
                .findAny().get();
        int y = first.getParameters().stream()
                .filter(p -> p.getName().equalsIgnoreCase("y"))
                .map(p -> Integer.parseInt(p.getGene().getValueAsRawString()))
                .findAny().get();

        //update SQL insertion with values close to the requested x/y
        insertions.stream()
                .flatMap(a -> a.seeTopGenes().stream())
                .forEach(g -> {
                    if (g.getName().equalsIgnoreCase("x")) {
                        IntegerGene gene = (IntegerGene) g;
                        gene.setValue(x + 1);
                    } else if (g.getName().equalsIgnoreCase("y")) {
                        IntegerGene gene = (IntegerGene) g;
                        gene.setValue(y + 1);
                    }
                });

        RestIndividual withSQL = (RestIndividual) ind.copy(); //new RestIndividual(ind.seeActions(), ind.getSampleType(), insertions, null, Traceable.DEFAULT_INDEX);
        withSQL.addInitializingDbActions(0,insertions);

        ei = ff.calculateCoverage(withSQL, noDataFV.getViewOfData().keySet(), null);
        assertNotNull(ei);

        //should have better heuristic
        FitnessValue closeDataFV = ei.getFitness();

        assertTrue(closeDataFV.averageExtraDistancesToMinimize(0) <
                noDataFV.averageExtraDistancesToMinimize(0));

        for (int target : noDataFV.getViewOfData().keySet()) {
            assertTrue(closeDataFV.compareExtraToMinimize(target, noDataFV, EMConfig.SecondaryObjectiveStrategy.AVG_DISTANCE) >= 0);
        }

        //but still not reaching target
        result = (RestCallResult) ((EvaluatedAction) ei.evaluatedMainActions().get(0)).getResult();
        assertEquals(400, result.getStatusCode().intValue());


        //finally, with correct data
        insertions.stream()
                .flatMap(a -> a.seeTopGenes().stream())
                .forEach(g -> {
                    if (g.getName().equalsIgnoreCase("x")) {
                        IntegerGene gene = (IntegerGene) g;
                        gene.setValue(x);
                    } else if (g.getName().equalsIgnoreCase("y")) {
                        IntegerGene gene = (IntegerGene) g;
                        gene.setValue(y);
                    }
                });

        ei = ff.calculateCoverage(withSQL, Collections.emptySet(), null);
        assertNotNull(ei);

        //As SQL data is returned, we get no heuristic, and so worst value
//        FitnessValue rightDataFV = ei.getFitness();
//
//        for(int target : closeDataFV.getViewOfData().keySet()) {
//            assertTrue(rightDataFV.compareExtraToMinimize(target, closeDataFV) >= 0);
//        }

        result = (RestCallResult) ((EvaluatedAction) ei.evaluatedMainActions().get(0)).getResult();
        assertEquals(200, result.getStatusCode().intValue());
    }


}
