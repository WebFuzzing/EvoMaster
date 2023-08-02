package org.evomaster.core.sql.schema

import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ColumnFactoryTest {


    @Test
    fun givenABigSerialColumnDtoWhenCreatingAColumnThenItHasTheCorrectType() {
        val columnDto = ColumnDto()
        columnDto.name = "product_id"
        columnDto.type = "bigserial"
        val databaseType = DatabaseType.POSTGRES

        val column = ColumnFactory.createColumnFromDto(columnDto = columnDto, lowerBoundForColumn = null, upperBoundForColumn = null,
                enumValuesForColumn = null, similarToPatternsForColumn = null, likePatternsForColumn = null,
                databaseType = databaseType)

        assertEquals(ColumnDataType.BIGSERIAL, column.type)
    }

    @Test
    fun givenASerialColumnDtoWhenCreatingAColumnThenItHasTheCorrectType() {
        val columnDto = ColumnDto()
        columnDto.name = "product_id"
        columnDto.type = "serial"
        val databaseType = DatabaseType.POSTGRES

        val column = ColumnFactory.createColumnFromDto(columnDto = columnDto, lowerBoundForColumn = null, upperBoundForColumn = null,
            enumValuesForColumn = null, similarToPatternsForColumn = null, likePatternsForColumn = null,
            databaseType = databaseType)

        assertEquals(ColumnDataType.SERIAL, column.type)
    }


    @Test
    fun givenAnInvalidColumnDtoWhenCreatingAColumnThenItThrowsIllegalArgumentException() {
        val columnDto = ColumnDto()
        columnDto.name = "product_id"
        columnDto.type = "invalid"
        val databaseType = DatabaseType.POSTGRES

        val assertionThrown = assertThrows<IllegalArgumentException> {
            ColumnFactory.createColumnFromDto(columnDto = columnDto, lowerBoundForColumn = null, upperBoundForColumn = null,
                    enumValuesForColumn = null, similarToPatternsForColumn = null, likePatternsForColumn = null,
                    databaseType = databaseType)
        }

        assertThat(assertionThrown.message, containsString("Column data type invalid is not supported"))
    }
}
