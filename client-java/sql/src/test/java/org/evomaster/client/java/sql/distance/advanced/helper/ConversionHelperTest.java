package org.evomaster.client.java.sql.distance.advanced.helper;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static java.lang.Double.MIN_VALUE;
import static org.evomaster.client.java.sql.distance.advanced.helpers.ConversionsHelper.convertToBoolean;
import static org.evomaster.client.java.sql.distance.advanced.helpers.ConversionsHelper.convertToDouble;

public class ConversionHelperTest {

    @Test
    public void testConvertToDouble() { //Number to double
        Assertions.assertEquals(1D, convertToDouble(1L), MIN_VALUE);
    }

    @Test
    public void testConvertToBoolean() { //Number to true boolean
        Assertions.assertEquals(true, convertToBoolean(1L));
        Assertions.assertEquals(true, convertToBoolean(2L));
    }

    @Test
    public void testConvertToBoolean2() { //Number to false boolean
        Assertions.assertEquals(false, convertToBoolean(0L));
    }
}