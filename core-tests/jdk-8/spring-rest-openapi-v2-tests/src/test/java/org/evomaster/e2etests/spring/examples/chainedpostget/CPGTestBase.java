package org.evomaster.e2etests.spring.examples.chainedpostget;

import com.foo.rest.examples.spring.chainedpostget.CPGController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class CPGTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new CPGController());
    }

}
