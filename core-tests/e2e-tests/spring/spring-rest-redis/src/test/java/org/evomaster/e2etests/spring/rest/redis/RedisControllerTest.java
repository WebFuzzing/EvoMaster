package org.evomaster.e2etests.spring.rest.redis;

import com.foo.spring.rest.redis.RedisController;
import com.foo.spring.rest.redis.lettuce.RedisLettuceAppController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RedisControllerTest {
    @Test
    public void testCanStartSut() {
        RedisController controller = new RedisLettuceAppController();
        controller.startSut();
        assertTrue(controller.isSutRunning());
    }
}
