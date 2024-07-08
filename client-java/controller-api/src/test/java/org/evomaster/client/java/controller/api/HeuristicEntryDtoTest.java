package org.evomaster.client.java.controller.api;

import org.evomaster.client.java.controller.api.dto.HeuristicEntryDto;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HeuristicEntryDtoTest {

    @Test
    public void testSerializationDeserealization() throws IOException, ClassNotFoundException {
        HeuristicEntryDto dto = new HeuristicEntryDto();
        dto.value = 10.0;
        dto.id = "Select * From Person Where Age>10";
        dto.type = HeuristicEntryDto.Type.SQL;
        dto.objective = HeuristicEntryDto.Objective.MINIMIZE_TO_ZERO;
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
        HeuristicEntryDto deserializedDto = (HeuristicEntryDto) objectInStream.readObject();

        assertEquals(10.0, deserializedDto.value);
        assertEquals("Select * From Person Where Age>10", deserializedDto.id);
        assertEquals(HeuristicEntryDto.Type.SQL, deserializedDto.type);
        assertEquals(HeuristicEntryDto.Objective.MINIMIZE_TO_ZERO, deserializedDto.objective);
        assertEquals(10, deserializedDto.numberOfEvaluatedRecords);
    }
}
