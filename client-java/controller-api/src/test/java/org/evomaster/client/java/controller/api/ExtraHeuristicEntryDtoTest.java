package org.evomaster.client.java.controller.api;

import org.evomaster.client.java.controller.api.dto.ExtraHeuristicEntryDto;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExtraHeuristicEntryDtoTest {

    @Test
    public void testSerializationDeserealization() throws IOException, ClassNotFoundException {
        ExtraHeuristicEntryDto dto = new ExtraHeuristicEntryDto();
        dto.value = 10.0;
        dto.id = "Select * From Person Where Age>10";
        dto.type = ExtraHeuristicEntryDto.Type.SQL;
        dto.objective = ExtraHeuristicEntryDto.Objective.MINIMIZE_TO_ZERO;
        dto.numberOfEvaluatedRecords = 10;

        // Serialize the DTO object
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream);
        objectOutStream.writeObject(dto);
        objectOutStream.flush();
        byte[] serializedBytes = byteOutStream.toByteArray();

        // Deserialize the DTO object
        ByteArrayInputStream byteInStream = new ByteArrayInputStream(serializedBytes);
        ObjectInputStream objectInStream = new ObjectInputStream(byteInStream);
        ExtraHeuristicEntryDto deserializedDto = (ExtraHeuristicEntryDto) objectInStream.readObject();

        assertEquals(10.0, deserializedDto.value);
        assertEquals("Select * From Person Where Age>10", deserializedDto.id);
        assertEquals(ExtraHeuristicEntryDto.Type.SQL, deserializedDto.type);
        assertEquals(ExtraHeuristicEntryDto.Objective.MINIMIZE_TO_ZERO, deserializedDto.objective);
        assertEquals(10, deserializedDto.numberOfEvaluatedRecords);
    }
}
