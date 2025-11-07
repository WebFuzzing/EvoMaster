package org.evomaster.e2etests.spring.rpc.examples.numericstring;

import com.foo.rpc.examples.spring.numericstring.NumericStringController;
import org.evomaster.e2etests.spring.rpc.examples.SpringRPCTestBase;
import org.junit.jupiter.api.BeforeAll;

public class NumericStringTestBase extends SpringRPCTestBase {
    @BeforeAll
    public static void initClass() throws Exception {
        SpringRPCTestBase.initClass(new NumericStringController());
    }
}
