package org.evomaster.e2etests.spring.examples.resource;

import com.foo.rest.examples.spring.resource.ResourceRestController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

/**
 * created by manzh on 2019-08-12
 */
public class ResourceTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ResourceRestController());
    }
}
