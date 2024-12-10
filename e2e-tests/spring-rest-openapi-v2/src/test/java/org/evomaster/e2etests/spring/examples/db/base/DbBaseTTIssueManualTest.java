package org.evomaster.e2etests.spring.examples.db.base;

import com.foo.rest.examples.spring.db.base.DbBaseDto;
import io.restassured.http.ContentType;
import org.evomaster.client.java.controller.api.dto.ActionDto;
import org.evomaster.client.java.controller.api.dto.StringSpecializationInfoDto;
import org.evomaster.client.java.controller.api.dto.TestResultsDto;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class DbBaseTTIssueManualTest extends DbBaseTestBase {


    @Test
    public void testExtraHeuristics(){

        String url = baseUrlOfSut + "/api/db/base/entities";

        String foo = "foo";
        String bar = "bar";

        remoteController.startANewSearch();

        ActionDto first = new ActionDto(); first.index = 0;
        remoteController.registerNewAction(first);
        given().contentType(ContentType.JSON)
                .body(new DbBaseDto(0l, foo))
                .post(url)
                .then()
                .statusCode(201);


        ActionDto second = new ActionDto(); second.index = 1;
        remoteController.registerNewAction(second);
        given().accept(ContentType.JSON)
                .get(url+"ByName/"+bar)
                .then()
                .statusCode(404);

        //make sure that the SQL Extra Heuristics is computed
        TestResultsDto result = remoteController.getTestResults(new HashSet<>(), true, false, false);
        assertFalse(result.extraHeuristics.isEmpty());
        assertFalse(result.extraHeuristics.get(second.index).heuristics.isEmpty());
    }


    @Test
    public void testTaintEqualInSql(){

        String url = baseUrlOfSut + "/api/db/base/entities";

        String foo = TaintInputName.getTaintName(0);
        String bar = TaintInputName.getTaintName(1);

        remoteController.startANewSearch();

        ActionDto first = new ActionDto(); first.index = 0;
        remoteController.registerNewAction(first);
        given().contentType(ContentType.JSON)
                .body(new DbBaseDto(0l, foo))
                .post(url)
                .then()
                .statusCode(201);


        ActionDto second = new ActionDto(); second.index = 1;
        remoteController.registerNewAction(second);
        given().accept(ContentType.JSON)
                .get(url+"ByName/"+bar)
                .then()
                .statusCode(404);

        //make sure that the SQL Extra Heuristics is computed
        TestResultsDto result = remoteController.getTestResults(new HashSet<>(), true, false, false);
        assertFalse(result.extraHeuristics.isEmpty());
        assertFalse(result.extraHeuristics.get(second.index).heuristics.isEmpty());

        //No EQUAL specialization in the first action
        List<StringSpecializationInfoDto> spec0 =  result.additionalInfoList.get(0).stringSpecializations.get(foo);
        //Man, it is weird: spec0 is null when running this test on local or CircleCI, but on CI, spec0 is not null.
        assertFalse(spec0 != null && spec0.stream().anyMatch(s -> s.stringSpecialization.equals(StringSpecialization.EQUAL.toString()))
                , "a number of additionalInfoList at index 0 is "+result.additionalInfoList.size());


        // In the second action, we should get 2 EQUAL, for both variables
        List<StringSpecializationInfoDto> spec1 =  result.additionalInfoList.get(1).stringSpecializations.get(foo);
        List<StringSpecializationInfoDto> z = spec1.stream()
                .filter(s -> s.stringSpecialization.equals(StringSpecialization.EQUAL.toString()))
                .collect(Collectors.toList());
        assertEquals(1, z.size());
        String fooValue = z.get(0).value;


        List<StringSpecializationInfoDto> spec2 =  result.additionalInfoList.get(1).stringSpecializations.get(bar);
        List<StringSpecializationInfoDto> w = spec2.stream()
                .filter(s -> s.stringSpecialization.equals(StringSpecialization.EQUAL.toString()))
                .collect(Collectors.toList());
        assertEquals(1, w.size());
        String barValue = w.get(0).value;

        assertEquals(fooValue, barValue);
    }
}
