package org.evomaster.e2etests.spring.examples.impactXYZ;

import com.foo.rest.examples.spring.impactXYZ.ImpactXYZRestController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;

/**
 * created by manzh on 2020-06-16
 */
public class ImpactXYZTestBase extends SpringTestBase  {

    @BeforeAll
    public static void initClass() throws Exception {
        SpringTestBase.initClass(new ImpactXYZRestController(Arrays.asList("/api/intImpact/{name}")));
    }
}
