package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.sql.SchemaExtractor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Created by jgaleotti on 02-May-22.
 */
class UnsupportedNumericCompositeTypeTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_unsupported_composite_type_numeric.sql"

    @Test
    fun testFailureToExtractSchema() {
        assertThrows<UnsupportedOperationException> { SchemaExtractor.extract(connection) }

    }


}