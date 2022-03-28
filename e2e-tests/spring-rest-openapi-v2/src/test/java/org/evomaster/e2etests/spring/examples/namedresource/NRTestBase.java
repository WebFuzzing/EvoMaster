package org.evomaster.e2etests.spring.examples.namedresource;

import com.foo.rest.examples.spring.namedresource.NamedResourceController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

/**
 * Created by arcand on 01.03.17.
 */
public class NRTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new NamedResourceController());
    }
}
