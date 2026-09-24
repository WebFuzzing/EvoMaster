package org.evomaster.e2etests.spring.rest.cassandra;

import com.foo.spring.rest.cassandra.CassandraController;
import com.foo.spring.rest.cassandra.findbyage.CassandraFindByAgeController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CassandraControllerTest {

    @Test
    public void testCanStartSut() {
        CassandraController controller = new CassandraFindByAgeController();
        controller.startSut();
        assertTrue(controller.isSutRunning());
    }
}