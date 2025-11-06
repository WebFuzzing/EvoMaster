package org.evomaster.e2etests.spring.rest.opensearch;

import com.foo.spring.rest.opensearch.OpenSearchController;
import com.foo.spring.rest.opensearch.findoneby.OpenSearchFindOneByController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenSearchControllerTest {
    @Test
    public void testCanStartSut() {
        OpenSearchController controller = new OpenSearchFindOneByController();
        controller.startSut();
        assertTrue(controller.isSutRunning());
    }
}
