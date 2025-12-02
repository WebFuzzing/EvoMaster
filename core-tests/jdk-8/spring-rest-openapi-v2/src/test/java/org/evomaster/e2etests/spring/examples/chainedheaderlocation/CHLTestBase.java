package org.evomaster.e2etests.spring.examples.chainedheaderlocation;

import com.foo.rest.examples.spring.chainedheaderlocation.CHLController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class CHLTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new CHLController());
    }

}
