package org.evomaster.e2etests.spring.examples.strings;

import com.foo.rest.examples.spring.strings.StringsController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class StringsTestBase extends SpringTestBase {


    @BeforeAll
    public static void initClass() throws Exception {

        StringsController controller = new StringsController();
        SpringTestBase.initClass(controller);
    }
}