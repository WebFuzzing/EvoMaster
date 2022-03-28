package org.evomaster.e2etests.spring.examples.db.javatypes;

import com.foo.rest.examples.spring.db.javatypes.JavaTypesController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class JavaTypesTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new JavaTypesController());
    }

}
