package org.evomaster.e2etests.spring.examples.expectations;

import com.foo.rest.examples.spring.expectations.ExpectationsController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class ExpectationsTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        ExpectationsController controller = new ExpectationsController();
        SpringTestBase.initClass(controller);
    }
}
