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
        addNode(DeclareConstSMTNode("products__1", "ProductsRow"))
        addNode(DeclareConstSMTNode("products__2", "ProductsRow"))
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
        addNode(DeclareConstSMTNode("users__1", "UsersRow"))
        addNode(DeclareConstSMTNode("users__2", "UsersRow"))
        addNode(
            AssertSMTNode(DistinctAssertion(listOf("(DOCUMENT users__1)", "(DOCUMENT users__2)")))
        )
        addNode(
            AssertSMTNode(
                EqualsAssertion(listOf("(NAME users__1)", "\"Alice\""))
            )
        )
        addNode(
            AssertSMTNode(
                EqualsAssertion(listOf("(NAME users__2)", "\"Alice\""))
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        LessThanAssertion("(POINTS users__1)", "4"),
                        GreaterThanAssertion("(POINTS users__1)", "6")
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        LessThanAssertion("(POINTS users__2)", "4"),
                        GreaterThanAssertion("(POINTS users__2)", "6")
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        GreaterThanAssertion("(AGE users__1)", "18"),
                        LessThanAssertion("(AGE users__1)", "100")
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        GreaterThanAssertion("(AGE users__2)", "18"),
                        LessThanAssertion("(AGE users__2)", "100")
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                LessThanOrEqualsAssertion("(POINTS users__1)", "10")
            )
        )
        addNode(
            AssertSMTNode(
                LessThanOrEqualsAssertion("(POINTS users__2)", "10")
            )
        )
        addNode(
            AssertSMTNode(
                GreaterThanOrEqualsAssertion("(POINTS users__1)", "0")
            )
        )
        addNode(
            AssertSMTNode(
                GreaterThanOrEqualsAssertion("(POINTS users__2)", "0")
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        EqualsAssertion(listOf("(USER_ID products__1)", "(ID users__1)")),
                        EqualsAssertion(listOf("(USER_ID products__1)", "(ID users__2)"))
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        EqualsAssertion(listOf("(USER_ID products__2)", "(ID users__1)")),
                        EqualsAssertion(listOf("(USER_ID products__2)", "(ID users__2)"))
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                DistinctAssertion(
                    listOf(
                        "(ID users__1)",
                        "(ID users__2)"
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        EqualsAssertion(listOf("(LUCKY users__1)", "\"true\"")),
                        EqualsAssertion(listOf("(LUCKY users__1)", "\"false\""))
                    )
                )
            )
        )
        addNode(
            AssertSMTNode(
                OrAssertion(
                    listOf(
                        EqualsAssertion(listOf("(LUCKY users__2)", "\"true\"")),
                        EqualsAssertion(listOf("(LUCKY users__2)", "\"false\""))
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
                    "ALTER TABLE users add CHECK (name = 'Alice');\n" +
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
     * Test that a <> (not-equals) condition in the WHERE clause is correctly translated
     * to a distinct SMT-LIB assertion.
     */
    @Test
    @Throws(JSQLParserException::class)
    fun selectFromUsersWithNotEquals() {
        val selectStatement: Statement = CCJSqlParserUtil.parse("SELECT * FROM Users WHERE age <> 30;")

        val response: SMTLib = generator.generateSMT(selectStatement)

        val expected = tableConstraints
        expected.addNode(AssertSMTNode(DistinctAssertion(listOf("(AGE users__1)", "30"))))
        expected.addNode(AssertSMTNode(DistinctAssertion(listOf("(AGE users__2)", "30"))))
        expected.addNode(CheckSatSMTNode())
        expected.addNode(GetValueSMTNode("users__1"))
        expected.addNode(GetValueSMTNode("users__2"))

        assertEquals(expected, response)
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
                        GreaterThanAssertion("(AGE users__1)", "30"),
                        EqualsAssertion(listOf("7", "(POINTS users__1)"))
                    )
                )
            )
        )
        expected.addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        GreaterThanAssertion("(AGE users__2)", "30"),
                        EqualsAssertion(listOf("7", "(POINTS users__2)"))
                    )
                )
            )
        )

        val satConstraints = arrayOf(
            CheckSatSMTNode(),
            GetValueSMTNode("users__1"),
            GetValueSMTNode("users__2")
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
                        GreaterThanAssertion("(AGE users__1)", "30"),
                        EqualsAssertion(listOf("(POINTS users__1)", "7"))
                    )
                )
            )
        )
        expected.addNode(
            AssertSMTNode(
                AndAssertion(
                    listOf(
                        GreaterThanAssertion("(AGE users__2)", "30"),
                        EqualsAssertion(listOf("(POINTS users__2)", "7"))
                    )
                )
            )
        )

        val satConstraints = arrayOf(
            CheckSatSMTNode(),
            GetValueSMTNode("users__1"),
            GetValueSMTNode("users__2")
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
                EqualsAssertion(listOf("(ID users__1)", "(USER_ID products__1)"))
            )
        )
        expected.addNode(
            AssertSMTNode(
                EqualsAssertion(listOf("(ID users__2)", "(USER_ID products__2)"))
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
                                        GreaterThanAssertion("(AGE users__1)", "30"),
                                        EqualsAssertion(listOf("(POINTS users__1)", "7"))
                                    )
                                ),
                            GreaterThanAssertion("(MIN_PRICE products__1)", "500")
                            )
                        ),
                        EqualsAssertion(listOf("(STOCK products__1)", "8"))
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
                                        GreaterThanAssertion("(AGE users__2)", "30"),
                                        EqualsAssertion(listOf("(POINTS users__2)", "7"))
                                    )
                                ),
                                GreaterThanAssertion("(MIN_PRICE products__2)", "500")
                            )
                        ),
                        EqualsAssertion(listOf("(STOCK products__2)", "8"))
                    )
                )
            )
        )

        val satConstraints = arrayOf(
            CheckSatSMTNode(),
            GetValueSMTNode("products__1"),
            GetValueSMTNode("products__2"),
            GetValueSMTNode("users__1"),
            GetValueSMTNode("users__2")
        )

        for (constraint in satConstraints) {
            expected.addNode(constraint)
        }

        assertEquals(expected, response)
    }

    /**
     * Test that NULL comparisons in the WHERE clause are skipped (not emitted as invalid SMT-LIB),
     * and the remaining non-null constraints are still applied.
     */
    @Test
    @Throws(JSQLParserException::class)
    fun selectFromUsersWithNullComparison() {
        val selectStatement: Statement = CCJSqlParserUtil.parse("SELECT * FROM Users WHERE name = NULL AND age > 30;")

        val response: SMTLib = generator.generateSMT(selectStatement)

        val expected = tableConstraints
        // The NULL comparison is skipped; only the non-null constraint is emitted
        expected.addNode(AssertSMTNode(GreaterThanAssertion("(AGE users__1)", "30")))
        expected.addNode(AssertSMTNode(GreaterThanAssertion("(AGE users__2)", "30")))

        val satConstraints = arrayOf(
            CheckSatSMTNode(),
            GetValueSMTNode("users__1"),
            GetValueSMTNode("users__2")
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
            expected.addNode(DeclareConstSMTNode("flags__1", "FlagsRow"))
            expected.addNode(DeclareConstSMTNode("flags__2", "FlagsRow"))
            // Primary key distinct constraint
            expected.addNode(AssertSMTNode(DistinctAssertion(listOf("(ID flags__1)", "(ID flags__2)"))))
            // Boolean value constraints (generated because H2 reports BIT as BOOLEAN)
            expected.addNode(
                AssertSMTNode(
                    OrAssertion(
                        listOf(
                            EqualsAssertion(listOf("(FLAG flags__1)", "\"true\"")),
                            EqualsAssertion(listOf("(FLAG flags__1)", "\"false\""))
                        )
                    )
                )
            )
            expected.addNode(
                AssertSMTNode(
                    OrAssertion(
                        listOf(
                            EqualsAssertion(listOf("(FLAG flags__2)", "\"true\"")),
                            EqualsAssertion(listOf("(FLAG flags__2)", "\"false\""))
                        )
                    )
                )
            )
            // Query constraint: id > 0
            expected.addNode(AssertSMTNode(GreaterThanAssertion("(ID flags__1)", "0")))
            expected.addNode(AssertSMTNode(GreaterThanAssertion("(ID flags__2)", "0")))
            expected.addNode(CheckSatSMTNode())
            expected.addNode(GetValueSMTNode("flags__1"))
            expected.addNode(GetValueSMTNode("flags__2"))

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
            expected.addNode(DeclareConstSMTNode("events__1", "EventsRow"))
            expected.addNode(DeclareConstSMTNode("events__2", "EventsRow"))
            // Primary key distinct constraint
            expected.addNode(AssertSMTNode(DistinctAssertion(listOf("(ID events__1)", "(ID events__2)"))))
            // Timestamp range constraints (Unix epoch start to year 3000 in seconds)
            expected.addNode(AssertSMTNode(GreaterThanOrEqualsAssertion("(CREATED_AT events__1)", "0")))
            expected.addNode(AssertSMTNode(LessThanOrEqualsAssertion("(CREATED_AT events__1)", "32503680000")))
            expected.addNode(AssertSMTNode(GreaterThanOrEqualsAssertion("(CREATED_AT events__2)", "0")))
            expected.addNode(AssertSMTNode(LessThanOrEqualsAssertion("(CREATED_AT events__2)", "32503680000")))
            // Query constraint: id > 0
            expected.addNode(AssertSMTNode(GreaterThanAssertion("(ID events__1)", "0")))
            expected.addNode(AssertSMTNode(GreaterThanAssertion("(ID events__2)", "0")))
            expected.addNode(CheckSatSMTNode())
            expected.addNode(GetValueSMTNode("events__1"))
            expected.addNode(GetValueSMTNode("events__2"))

            assertEquals(expected, response)
        } finally {
            conn.close()
        }
    }

    @Test
    @Throws(JSQLParserException::class)
    fun compositePkEmitsDisjunctiveDistinctness() {
        val conn = DriverManager.getConnection("jdbc:h2:mem:composite_pk_test", "sa", "")
        try {
            SqlScriptRunner.execCommand(
                conn,
                "CREATE TABLE assignments(employee_id int not null, project_id int not null, PRIMARY KEY (employee_id, project_id));"
            )
            val schemaDto = DbInfoExtractor.extract(conn)
            val compositePkGenerator = SmtLibGenerator(schemaDto, 2)

            val selectStatement: Statement = CCJSqlParserUtil.parse("SELECT * FROM assignments")
            val response: SMTLib = compositePkGenerator.generateSMT(selectStatement)

            val expected = SMTLib()
            expected.addNode(
                DeclareDatatypeSMTNode(
                    "AssignmentsRow", ImmutableList.of(
                        DeclareConstSMTNode("EMPLOYEE_ID", "Int"),
                        DeclareConstSMTNode("PROJECT_ID", "Int")
                    )
                )
            )
            expected.addNode(DeclareConstSMTNode("assignments__1", "AssignmentsRow"))
            expected.addNode(DeclareConstSMTNode("assignments__2", "AssignmentsRow"))
            // Composite PK: at least one column must differ between row pairs — not each column individually.
            expected.addNode(AssertSMTNode(OrAssertion(listOf(
                DistinctAssertion(listOf("(EMPLOYEE_ID assignments__1)", "(EMPLOYEE_ID assignments__2)")),
                DistinctAssertion(listOf("(PROJECT_ID assignments__1)", "(PROJECT_ID assignments__2)"))
            ))))
            expected.addNode(CheckSatSMTNode())
            expected.addNode(GetValueSMTNode("assignments__1"))
            expected.addNode(GetValueSMTNode("assignments__2"))

            assertEquals(expected, response)
        } finally {
            conn.close()
        }
    }

    @Test
    @Throws(JSQLParserException::class)
    fun deleteFromUsersWithWhereClause() {
        val deleteStatement: Statement = CCJSqlParserUtil.parse("DELETE FROM users WHERE age > 30")

        val response: SMTLib = generator.generateSMT(deleteStatement)

        val expected = tableConstraints
        expected.addNode(AssertSMTNode(GreaterThanAssertion("(AGE users__1)", "30")))
        expected.addNode(AssertSMTNode(GreaterThanAssertion("(AGE users__2)", "30")))
        expected.addNode(CheckSatSMTNode())
        expected.addNode(GetValueSMTNode("users__1"))
        expected.addNode(GetValueSMTNode("users__2"))

        assertEquals(expected, response)
    }

    @Test
    @Throws(JSQLParserException::class)
    fun updateUsersWithWhereClause() {
        val updateStatement: Statement = CCJSqlParserUtil.parse("UPDATE users SET points = 5 WHERE age > 30")

        val response: SMTLib = generator.generateSMT(updateStatement)

        val expected = tableConstraints
        expected.addNode(AssertSMTNode(GreaterThanAssertion("(AGE users__1)", "30")))
        expected.addNode(AssertSMTNode(GreaterThanAssertion("(AGE users__2)", "30")))
        expected.addNode(CheckSatSMTNode())
        expected.addNode(GetValueSMTNode("users__1"))
        expected.addNode(GetValueSMTNode("users__2"))

        assertEquals(expected, response)
    }
}
