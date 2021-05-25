package org.evomaster.e2etests.spring.examples.webrequest;

import com.foo.rest.examples.spring.strings.StringsController;
import com.foo.rest.examples.spring.webrequest.WebRequestController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class WebRequestTestBase extends SpringTestBase {


    @BeforeAll
    public static void initClass() throws Exception {

        WebRequestController controller = new WebRequestController();
        SpringTestBase.initClass(controller);
    }
}
