package org.evomaster.core.sql.extract.postgres

import org.evomaster.sql.internal.SchemaExtractor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Created by jgaleotti on 02-May-22.
 */
class UnsupportedVarbitCompositeTypeTest : ExtractTestBasePostgres() {

    override fun getSchemaLocation() = "/sql_schema/postgres_unsupported_composite_type_varbit.sql"

    @Test
    fun testFailureToExtractSchema() {
        assertThrows<UnsupportedOperationException> { org.evomaster.sql.internal.SchemaExtractor.extract(connection) }

    }


}