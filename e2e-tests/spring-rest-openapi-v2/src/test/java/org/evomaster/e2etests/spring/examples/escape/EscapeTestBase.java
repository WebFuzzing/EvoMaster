package org.evomaster.e2etests.spring.examples.escape;

import com.foo.rest.examples.spring.escape.EscapeController;
import com.foo.rest.examples.spring.strings.StringsController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class EscapeTestBase extends SpringTestBase {


    @BeforeAll
    public static void initClass() throws Exception {

        EscapeController controller = new EscapeController();
        SpringTestBase.initClass(controller);
    }
}