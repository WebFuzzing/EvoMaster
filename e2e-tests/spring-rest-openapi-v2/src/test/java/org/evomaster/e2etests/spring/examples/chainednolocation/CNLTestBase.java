package org.evomaster.e2etests.spring.examples.chainednolocation;

import com.foo.rest.examples.spring.chainednolocation.CNLController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class CNLTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new CNLController());
    }

}
