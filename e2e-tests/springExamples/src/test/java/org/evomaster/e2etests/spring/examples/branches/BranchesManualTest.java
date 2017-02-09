package org.evomaster.e2etests.spring.examples.branches;

import com.foo.rest.examples.spring.branches.BranchesController;
import com.foo.rest.examples.spring.branches.BranchesPostDto;
import io.restassured.http.ContentType;
import org.evomaster.clientJava.controllerApi.dto.TargetsResponseDto;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BranchesManualTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        BranchesController controller = new BranchesController();
        SpringTestBase.initClass(controller);
    }

    @BeforeEach
    public void reset(){
        //need this, otherwise tests would not be independent
        remoteController.startANewSearch();
    }

    @Test
    public void test(){

        TargetsResponseDto dto = remoteController.getTargetCoverage(Collections.emptySet());
        assertEquals(0, dto.targets.size());

        given().contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(new BranchesPostDto(5,0))
                .post(baseUrlOfSut + "/api/branches/pos")
                .then()
                .statusCode(200)
                .body("value", is(0));

        dto = remoteController.getTargetCoverage(Collections.emptySet());
        assertTrue(dto.targets.size() > 0);

        List<String> targetDescriptions = dto.targets.stream()
                .map(t -> t.descriptiveId)
                .sorted()
                .collect(Collectors.toList());

        String msg = "TARGETS:\n" + String.join("\n", targetDescriptions);

        assertTrue(targetDescriptions.stream().anyMatch(d -> d.contains("BranchesRest")), msg);
        assertTrue(targetDescriptions.stream().anyMatch(d -> d.contains("BranchesPostDto")), msg);
        assertTrue(targetDescriptions.stream().anyMatch(d -> d.contains("BranchesImp")), msg);
        assertTrue(targetDescriptions.stream().anyMatch(d -> d.startsWith("Line_")), msg);
        assertTrue(targetDescriptions.stream().anyMatch(d -> d.startsWith("Branch_")), msg);

        //FIXME: this pass on its own, but fail when tests run in sequence... need to find why
        //assertTrue(targetDescriptions.stream().anyMatch(d -> d.contains("BranchesResponseDto")), msg);


        /*
            tricky: this does not get instrumented.
            possible theory is that, being used directly from a org.evomaster class,
            it gets loaded before agent kicks in???
         */
        assertTrue(! targetDescriptions.stream().anyMatch(d -> d.contains("BranchesApplication")), msg);
    }
}
