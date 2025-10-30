package org.evomaster.e2etests.spring.examples.headerlocation;

import com.foo.rest.examples.spring.headerlocation.HeaderLocationController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class HLTestBase extends SpringTestBase{

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new HeaderLocationController());
    }

}
