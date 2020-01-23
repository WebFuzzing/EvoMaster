package org.evomaster.client.java.controller.internal.db.mongo;

import org.evomaster.client.java.controller.api.dto.database.execution.MongoExecutionDto;
import org.evomaster.client.java.controller.api.dto.database.execution.MongoOperationDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MongoHandlerTest {

    @Test
    public void testIsExtractMongoExecution() {
        MongoHandler mongoHandler = new MongoHandler();
        assertFalse(mongoHandler.isExtractMongoExecution());
        mongoHandler.setExtractMongoExecution(true);
        assertTrue(mongoHandler.isExtractMongoExecution());
    }

    @Test
    public void testHandle() {
        MongoHandler mongoHandler = new MongoHandler();
        mongoHandler.setExtractMongoExecution(true);
        mongoHandler.handle("{\"databaseName\":\"mydb\",\"collectionName\":\"mycollection\",\"query\":{}}\n");
        MongoExecutionDto dto = mongoHandler.getMongoExecutionDto();
        assertEquals(1, dto.mongoOperations.size());
        assertEquals(MongoOperationDto.Type.MONGO_FIND, dto.mongoOperations.get(0).operationType);
        assertEquals("{\"databaseName\":\"mydb\",\"collectionName\":\"mycollection\",\"query\":{}}\n", dto.mongoOperations.get(0).operationJsonStr);
    }

    @Test
    public void testReset() {
        MongoHandler mongoHandler = new MongoHandler();
        mongoHandler.setExtractMongoExecution(true);
        assertEquals(0, mongoHandler.getMongoExecutionDto().mongoOperations.size());

        mongoHandler.handle("{\"databaseName\":\"mydb\",\"collectionName\":\"mycollection\",\"query\":{}}\n");
        assertEquals(1, mongoHandler.getMongoExecutionDto().mongoOperations.size());

        mongoHandler.reset();
        assertEquals(0, mongoHandler.getMongoExecutionDto().mongoOperations.size());
    }
}
