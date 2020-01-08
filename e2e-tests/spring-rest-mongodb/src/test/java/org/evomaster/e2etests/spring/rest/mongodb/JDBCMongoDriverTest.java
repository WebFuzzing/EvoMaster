package org.evomaster.e2etests.spring.rest.mongodb;

import org.junit.jupiter.api.Test;

public class JDBCMongoDriverTest {

    @Test
    public void testLoad() throws ClassNotFoundException {
        Class.forName("mongodb.jdbc.MongoDriver");
    }
}
