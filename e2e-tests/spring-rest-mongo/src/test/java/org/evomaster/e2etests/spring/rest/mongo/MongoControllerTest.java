package org.evomaster.e2etests.spring.rest.mongo;

import com.foo.spring.rest.mongo.MongoController;
import com.foo.spring.rest.mongo.MongoStudentsAppController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MongoControllerTest {
    @Test
    public void testCanStartSut() {
        MongoController controller = new MongoStudentsAppController();
        controller.startSut();
        assertTrue(controller.isSutRunning());
    }
}
