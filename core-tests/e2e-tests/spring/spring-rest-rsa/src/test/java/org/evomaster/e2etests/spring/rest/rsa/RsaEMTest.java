package org.evomaster.e2etests.spring.rest.rsa;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.demo.vo.BindCardReq;
import com.example.demo.vo.CommonReq;
import org.evomaster.core.problem.rest.data.HttpVerb;
import org.evomaster.core.problem.rest.data.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class RsaEMTest extends RestTestBase {


    @BeforeAll
    public static void initClass() throws Exception {
        RestTestBase.initClass(new RsaController());
    }


    @Test
    public void testRunEM() throws Throwable {

        runTestHandlingFlakyAndCompilation(
                "RsaEM",
                500,
                50,
                (args) -> {

                    //UUID.randomUUID() makes assertions flaky, which we don't handle yet
                    setOption(args,"enableBasicAssertions", "false");

                    Solution<RestIndividual> solution = initAndRun(args);

                    // handle RSA encryption, but no data in DB
                    assertHasAtLeastOne(solution, HttpVerb.POST, 404, "/api/bind_card_apply", null);
                    // handle DB
                    assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bind_card_apply", null);
                }
        );
    }

}