package org.evomaster.client.java.sql;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataRowTest {

    @Test
    public void getValueByNameThrowsWhenDescriptorIsNull() {
        List<VariableDescriptor> descriptors = new ArrayList<>();
        VariableDescriptor baseTableVarDescriptor = new VariableDescriptor("id", "id", "public.product", "product0_");
        VariableDescriptor derivedTableVarDescriptor = new VariableDescriptor("id", "id", null);
        descriptors.add(baseTableVarDescriptor);
        descriptors.add(baseTableVarDescriptor);
        DataRow row = new DataRow(Arrays.asList(baseTableVarDescriptor, derivedTableVarDescriptor),
                Arrays.asList(1L, 2L));

        Object value = row.getValueByName("id", "product");
        assertEquals(1l, value);
    }


}
