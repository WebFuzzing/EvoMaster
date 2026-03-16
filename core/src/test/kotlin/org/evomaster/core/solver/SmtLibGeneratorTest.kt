package org.evomaster.core.solver

import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.solver.smtlib.*
import org.evomaster.solver.smtlib.assertion.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.com.google.common.collect.ImmutableList
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class SmtLibGeneratorTest {

    val tableConstraints = SMTLib().apply {
        addNode(
            DeclareDatatypeSMTNode(
                "ProductsRow", ImmutableList.of(
                    DeclareConstSMTNode("PRICE", "Int"),
                    DeclareConstSMTNode("MIN_PRICE", "Int"),
                    DeclareConstSMTNode("STOCK", "Int"),
                    DeclareConstSMTNode("USER_ID", "Int")
                )
            )
        )
        addNode(DeclareConstSMTNode("products1", "ProductsRow"))
        addNode(DeclareConstSMTNode("products2", "ProductsRow"))
        addNode(
            DeclareDatatypeSMTNode(
                "UsersRow", ImmutableList.of(
                    DeclareConstSMTNode("ID", "Int"),
                    DeclareConstSMTNode("DOCUMENT", "Int"),
                    DeclareConstSMTNode("NAME", "String"),
                    DeclareConstSMTNode("AGE", "Int"),
                    DeclareConstSMTNode("POINTS", "Int"),
                    DeclareConstSMTNode("LUCKY", "String")
                )
            )
        )
        addNode(DeclareConstSMTNode("users1", "UsersRow"))
        addNode(DeclareConstSMTNode("users2", "UsersRow"))
        addNode(
            AssertSMTNode(DistinctAssertion(listOf("(DOCUMENT users1)", "(DOCUMENT users2)")))
        )
        addNode(
            AssertSMTNode(
                EqualsAssertion(listOf("(NAME users1)", "\"agus\""))
            )
        )
        addNode(
            AssertSMTNode(
                EqualsAssertion(listOf("(NAME users2)", "\"agus\""))
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        LessThanAssertion("(POINTS users1)", "4"),
                        GreaterThanAssertion("(POINTS users1)", "6")
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        LessThanAssertion("(POINTS users2)", "4"),
                        GreaterThanAssertion("(POINTS users2)", "6")
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        GreaterThanAssertion("(AGE users1)", "18"),
                        LessThanAssertion("(AGE users1)", "100")
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        GreaterThanAssertion("(AGE users2)", "18"),
                        LessThanAssertion("(AGE users2)", "100")
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                LessThanOrEqualsAssertion("(POINTS users1)", "10")
            )
        )
        addNode(
            AssertSMTNode(
                LessThanOrEqualsAssertion("(POINTS users2)", "10")
            )
        )
        addNode(
            AssertSMTNode(
                GreaterThanOrEqualsAssertion("(POINTS users1)", "0")
            )
        )
        addNode(
            AssertSMTNode(
                GreaterThanOrEqualsAssertion("(POINTS users2)", "0")
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        EqualsAssertion(listOf("(USER_ID products1)", "(ID users1)")),
                        EqualsAssertion(listOf("(USER_ID products1)", "(ID users2)"))
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        EqualsAssertion(listOf("(USER_ID products2)", "(ID users1)")),
                        EqualsAssertion(listOf("(USER_ID products2)", "(ID users2)"))
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                DistinctAssertion(
                    listOf(
                        "(ID users1)",
                        "(ID users2)"
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        EqualsAssertion(listOf("(LUCKY users1)", "\"true\"")),
                        EqualsAssertion(listOf("(LUCKY users1)", "\"True\"")),
                        EqualsAssertion(listOf("(LUCKY users1)", "\"TRUE\"")),
                        EqualsAssertion(listOf("(LUCKY users1)", "\"false\"")),
                        EqualsAssertion(listOf("(LUCKY users1)", "\"False\"")),
                        EqualsAssertion(listOf("(LUCKY users1)", "\"FALSE\""))
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        EqualsAssertion(listOf("(LUCKY users2)", "\"true\"")),
                        EqualsAssertion(listOf("(LUCKY users2)", "\"True\"")),
                        EqualsAssertion(listOf("(LUCKY users2)", "\"TRUE\"")),
                        EqualsAssertion(listOf("(LUCKY users2)", "\"false\"")),
                        EqualsAssertion(listOf("(LUCKY users2)", "\"False\"")),
                        EqualsAssertion(listOf("(LUCKY users2)", "\"FALSE\""))
                    )
                )
            )
        )
    }

    companion object {
        private lateinit var generator: SmtLibGenerator
        private lateinit var connection: Connection

        @JvmStatic
        @BeforeAll
        @Throws(Exception::class)
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:constraint_test", "sa", "")
            SqlScriptRunner.execCommand(connection,"CREATE TABLE users(id bigint generated by default as identity primary key, document int, name varchar(255), age int, points int, lucky boolean);\n" +
                    "ALTER TABLE users add CHECK (age>18 AND age<100);\n" +
                    "ALTER TABLE users add CHECK (points<=10);\n" +
                    "ALTER TABLE users add CHECK (points>=0);\n" +
                    "ALTER TABLE users add CHECK (points<4 OR points>6);\n" +
                    "ALTER TABLE users add CHECK (name = 'agus');\n" +
                    "ALTER TABLE users ADD UNIQUE (document);\n" +
                    "CREATE TABLE products(price int not null, min_price int not null, stock int not null, user_id bigint not null);\n" +
                    "ALTER TABLE products add constraint userIdKey foreign key (user_id) REFERENCES users;\n")
            val schemaDto = DbInfoExtractor.extract(connection)

            generator = SmtLibGenerator(schemaDto, 2)
        }

        @JvmStatic
        @AfterAll
        @Throws(SQLException::class)
        fun tearDown() {
            connection.close()
        }
    }

    /**
     * Test the generation of SMT from a simple select statement and a database schema
     * @throws JSQLParserException if the statement is not valid
     */
    @Test
    @Throws(JSQLParserException::class)
    fun selectFromUsers() {
        val selectStatement: Statement = CCJSqlParserUtil.parse("SELECT * FROM Users WHERE Age > 30 AND 7 = points;")

        val response: SMTLib = generator.generateSMT(selectStatement)

        val expected = tableConstraints
        // Query constraints
        expected.addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        GreaterThanAssertion("(AGE users1)", "30"),
                        EqualsAssertion(listOf("7", "(POINTS users1)"))
                    )
                )
            )
        )
        expected.addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        GreaterThanAssertion("(AGE users2)", "30"),
                        EqualsAssertion(listOf("7", "(POINTS users2)"))
                    )
                )
            )
        )

        val satConstraints = arrayOf(
            CheckSatSMTNode(),
            GetValueSMTNode("users1"),
            GetValueSMTNode("users2")
        )

        for (constraint in satConstraints) {
            expected.addNode(constraint)
        }

        assertEquals(expected, response)
    }

    /**
     * Test the generation of SMT from a simple select statement and a database schema
     * @throws JSQLParserException if the statement is not valid
     */
    @Test
    @Throws(JSQLParserException::class)
    fun selectFromUsersWithTableAlias() {
        val selectStatement: Statement = CCJSqlParserUtil.parse("SELECT * FROM Users u WHERE u.Age > 30 AND u.points = 7;")

        val response: SMTLib = generator.generateSMT(selectStatement)
        val expected = tableConstraints
        // Query constraints
        expected.addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        GreaterThanAssertion("(AGE users1)", "30"),
                        EqualsAssertion(listOf("(POINTS users1)", "7"))
                    )
                )
            )
        )
        expected.addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        GreaterThanAssertion("(AGE users2)", "30"),
                        EqualsAssertion(listOf("(POINTS users2)", "7"))
                    )
                )
            )
        )

        val satConstraints = arrayOf(
            CheckSatSMTNode(),
            GetValueSMTNode("users1"),
            GetValueSMTNode("users2")
        )

        for (constraint in satConstraints) {
            expected.addNode(constraint)
        }

        assertEquals(expected, response)
    }

    /**
     * Test the generation of SMT from a simple select statement and a database schema
     * @throws JSQLParserException if the statement is not valid
     */
    @Test
    @Throws(JSQLParserException::class)
    fun selectFromUsersWithJoin() {
        val selectStatement: Statement = CCJSqlParserUtil.parse("SELECT * FROM Users u JOIN Products p ON u.id = p.user_id WHERE u.Age > 30 AND u.points = 7 AND p.min_price > 500 AND p.stock = 8;")

        val response: SMTLib = generator.generateSMT(selectStatement)
        val expected = tableConstraints

        // JOIN ON constraints
        expected.addNode(
            AssertSMTNode(
                EqualsAssertion(listOf("(ID users1)", "(USER_ID products1)"))
            )
        )
        expected.addNode(
            AssertSMTNode(
                EqualsAssertion(listOf("(ID users2)", "(USER_ID products2)"))
            )
        )

        // Query constraints
        expected.addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        AndAssertion(
                            listOf(
                                AndAssertion(
                                    listOf(
                                        GreaterThanAssertion("(AGE users1)", "30"),
                                        EqualsAssertion(listOf("(POINTS users1)", "7"))
                                    )
                                ),
                            GreaterThanAssertion("(MIN_PRICE products1)", "500")
                            )
                        ),
                        EqualsAssertion(listOf("(STOCK products1)", "8"))
                    )
                )
            )
        )
        expected.addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        AndAssertion(
                            listOf(
                                AndAssertion(
                                    listOf(
                                        GreaterThanAssertion("(AGE users2)", "30"),
                                        EqualsAssertion(listOf("(POINTS users2)", "7"))
                                    )
                                ),
                                GreaterThanAssertion("(MIN_PRICE products2)", "500")
                            )
                        ),
                        EqualsAssertion(listOf("(STOCK products2)", "8"))
                    )
                )
            )
        )

        val satConstraints = arrayOf(
            CheckSatSMTNode(),
            GetValueSMTNode("products1"),
            GetValueSMTNode("products2"),
            GetValueSMTNode("users1"),
            GetValueSMTNode("users2")
        )

        for (constraint in satConstraints) {
            expected.addNode(constraint)
        }

        assertEquals(expected, response)
    }

    /**
     * Test that a BIT column type is translated to Int in the SMT-LIB representation.
     */
    @Test
    @Throws(JSQLParserException::class)
    fun selectFromTableWithBitColumn() {
        val conn = DriverManager.getConnection("jdbc:h2:mem:bit_test", "sa", "")
        try {
            SqlScriptRunner.execCommand(
                conn,
                "CREATE TABLE flags(id bigint generated by default as identity primary key, flag bit);"
            )
            val schemaDto = DbInfoExtractor.extract(conn)
            val bitGenerator = SmtLibGenerator(schemaDto, 2)

            val selectStatement: Statement = CCJSqlParserUtil.parse("SELECT * FROM flags WHERE id > 0")
            val response: SMTLib = bitGenerator.generateSMT(selectStatement)

            val expected = SMTLib()
            // H2 maps BIT to BOOLEAN, which maps to String in SMT-LIB
            expected.addNode(
                DeclareDatatypeSMTNode(
                    "FlagsRow", ImmutableList.of(
                        DeclareConstSMTNode("ID", "Int"),
                        DeclareConstSMTNode("FLAG", "String")
                    )
                )
            )
            expected.addNode(DeclareConstSMTNode("flags1", "FlagsRow"))
            expected.addNode(DeclareConstSMTNode("flags2", "FlagsRow"))
            // Primary key distinct constraint
            expected.addNode(AssertSMTNode(DistinctAssertion(listOf("(ID flags1)", "(ID flags2)"))))
            // Boolean value constraints (generated because H2 reports BIT as BOOLEAN)
            expected.addNode(
                AssertSMTNode(
                    OrAssertion(
                        listOf(
                            EqualsAssertion(listOf("(FLAG flags1)", "\"true\"")),
                            EqualsAssertion(listOf("(FLAG flags1)", "\"True\"")),
                            EqualsAssertion(listOf("(FLAG flags1)", "\"TRUE\"")),
                            EqualsAssertion(listOf("(FLAG flags1)", "\"false\"")),
                            EqualsAssertion(listOf("(FLAG flags1)", "\"False\"")),
                            EqualsAssertion(listOf("(FLAG flags1)", "\"FALSE\""))
                        )
                    )
                )
            )
            expected.addNode(
                AssertSMTNode(
                    OrAssertion(
                        listOf(
                            EqualsAssertion(listOf("(FLAG flags2)", "\"true\"")),
                            EqualsAssertion(listOf("(FLAG flags2)", "\"True\"")),
                            EqualsAssertion(listOf("(FLAG flags2)", "\"TRUE\"")),
                            EqualsAssertion(listOf("(FLAG flags2)", "\"false\"")),
                            EqualsAssertion(listOf("(FLAG flags2)", "\"False\"")),
                            EqualsAssertion(listOf("(FLAG flags2)", "\"FALSE\""))
                        )
                    )
                )
            )
            // Query constraint: id > 0
            expected.addNode(AssertSMTNode(GreaterThanAssertion("(ID flags1)", "0")))
            expected.addNode(AssertSMTNode(GreaterThanAssertion("(ID flags2)", "0")))
            expected.addNode(CheckSatSMTNode())
            expected.addNode(GetValueSMTNode("flags1"))
            expected.addNode(GetValueSMTNode("flags2"))

            assertEquals(expected, response)
        } finally {
            conn.close()
        }
    }

    /**
     * Test that a TIMESTAMP column type is translated to Int in the SMT-LIB representation,
     * and that the timestamp range constraints (Unix epoch to year 3000) are generated.
     */
    @Test
    @Throws(JSQLParserException::class)
    fun selectFromTableWithTimestampColumn() {
        val conn = DriverManager.getConnection("jdbc:h2:mem:timestamp_test", "sa", "")
        try {
            SqlScriptRunner.execCommand(
                conn,
                "CREATE TABLE events(id bigint generated by default as identity primary key, created_at timestamp not null);"
            )
            val schemaDto = DbInfoExtractor.extract(conn)
            val timestampGenerator = SmtLibGenerator(schemaDto, 2)

            val selectStatement: Statement = CCJSqlParserUtil.parse("SELECT * FROM events WHERE id > 0")
            val response: SMTLib = timestampGenerator.generateSMT(selectStatement)

            val expected = SMTLib()
            expected.addNode(
                DeclareDatatypeSMTNode(
                    "EventsRow", ImmutableList.of(
                        DeclareConstSMTNode("ID", "Int"),
                        DeclareConstSMTNode("CREATED_AT", "Int")
                    )
                )
            )
            expected.addNode(DeclareConstSMTNode("events1", "EventsRow"))
            expected.addNode(DeclareConstSMTNode("events2", "EventsRow"))
            // Primary key distinct constraint
            expected.addNode(AssertSMTNode(DistinctAssertion(listOf("(ID events1)", "(ID events2)"))))
            // Timestamp range constraints (Unix epoch start to year 3000 in seconds)
            expected.addNode(AssertSMTNode(GreaterThanOrEqualsAssertion("(CREATED_AT events1)", "0")))
            expected.addNode(AssertSMTNode(LessThanOrEqualsAssertion("(CREATED_AT events1)", "32503680000")))
            expected.addNode(AssertSMTNode(GreaterThanOrEqualsAssertion("(CREATED_AT events2)", "0")))
            expected.addNode(AssertSMTNode(LessThanOrEqualsAssertion("(CREATED_AT events2)", "32503680000")))
            // Query constraint: id > 0
            expected.addNode(AssertSMTNode(GreaterThanAssertion("(ID events1)", "0")))
            expected.addNode(AssertSMTNode(GreaterThanAssertion("(ID events2)", "0")))
            expected.addNode(CheckSatSMTNode())
            expected.addNode(GetValueSMTNode("events1"))
            expected.addNode(GetValueSMTNode("events2"))

            assertEquals(expected, response)
        } finally {
            conn.close()
        }
    }
}
