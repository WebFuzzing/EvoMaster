package com.foo.spring.rest.h2.columntypes;

import com.foo.spring.rest.h2.SpringController;
import kotlin.random.Random;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbSpecification;
import org.hibernate.dialect.H2Dialect;
import org.springframework.boot.SpringApplication;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class H2ColumnTypesController extends SpringController {

    private static final String CREATE_TABLE_CHARACTER_TYPES = "CREATE TABLE characterTypes (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  characterColumn CHARACTER (5) NOT NULL,\n" +
            "  charColumn CHAR (5) NOT NULL,\n" +
            "  nationalCharColumn NATIONAL CHAR (5) NOT NULL,\n " +
            "  nationalCharacterColumn NATIONAL CHARACTER (5) NOT NULL,\n " +
            "  ncharColumn NCHAR (5) NOT NULL\n " +
            ");";

    private static final String CREATE_TABLE_CHARACTER_VARYING_TYPES = "CREATE TABLE characterVaryingTypes (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  characterVaryingColumn CHARACTER VARYING (5) NOT NULL,\n" +
            "  charVaryingColumn CHAR VARYING (5) NOT NULL,\n" +
            "  varcharColumn VARCHAR (5) NOT NULL,\n" +
            "  nationalCharacterVaryingColumn NATIONAL CHARACTER VARYING (5) NOT NULL,\n " +
            "  nationalCharVaryingColumn NATIONAL CHAR VARYING (5) NOT NULL,\n " +
            "  ncharVaryingColumn NCHAR VARYING (5) NOT NULL,\n " +
            "  varcharCasesensitiveColumn VARCHAR_CASESENSITIVE (5) NOT NULL\n " +
            ");";

    private static final String CREATE_TABLE_CHARACTER_LARGE_OBJECT_TYPES = "CREATE TABLE characterLargeObjectTypes (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  characterLargeObjectColumn CHARACTER LARGE OBJECT (10) NOT NULL,\n" +
            "  charLargeObjectColumn CHAR LARGE OBJECT (10) NOT NULL,\n" +
            "  clobColumn CLOB (10)NOT NULL,\n" +
            "  nationalCharacterLargeObjectColumn NATIONAL CHARACTER LARGE OBJECT (10) NOT NULL,\n" +
            "  ncharLargeObjectColumn NCHAR LARGE OBJECT(10) NOT NULL,\n " +
            "  nclobColumn NCLOB (10) NOT NULL\n " +
            ");";

    private static final String CREATE_TABLE_VARCHAR_IGNORE_CASE = "CREATE TABLE varcharIgnoreCaseType (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  varCharIgnoreCaseColumn VARCHAR_IGNORECASE (10) NOT NULL\n" +
            ");";


    private static final String CREATE_TABLE_BINARY_TYPES = "CREATE TABLE binaryTypes (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  binaryColumn BINARY (10) NOT NULL,\n" +
            "  binaryVaryingColumn BINARY VARYING (10) NOT NULL,\n" +
            "  varbinaryColumn VARBINARY (10) NOT NULL,\n" +
            "  binaryLargeObjectColumn BINARY LARGE OBJECT (10) NOT NULL,\n" +
            "  blobColumn BLOB (10) NOT NULL\n" +
            ");";

    private static final String CREATE_TABLE_NUMERIC_TYPES = "CREATE TABLE numericTypes (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            //"  decFloatColumn DECFLOAT NOT NULL,\n" +
            //"  decFloatWithPrecisionColumn DECFLOAT (20) NOT NULL,\n" +
            //"  floatWithPrecisionColumn FLOAT (2) NOT NULL,\n" +
            //"  numericWithPrecisionAndScaleColumn NUMERIC (20,2) NOT NULL,\n" +
            "  booleanColumn BOOLEAN NOT NULL,\n" +
            "  tinyintColumn TINYINT  NOT NULL,\n" +
            "  smallintColumn SMALLINT NOT NULL,\n" +
            "  integerColumn INTEGER NOT NULL,\n" +
            "  intColumn INT NOT NULL,\n" +
            "  bigintColumn BIGINT NOT NULL,\n" +
            "  numericColumn NUMERIC NOT NULL,\n" +
            "  decimalColumn DECIMAL NOT NULL,\n" +
            "  decColumn DEC NOT NULL,\n" +
            "  realColumn REAL NOT NULL,\n" +
            "  doublePrecisionColumn DOUBLE PRECISION NOT NULL,\n" +
            "  floatColumn FLOAT NOT NULL\n" +
            ");";

    private static final String CREATE_TABLE_DATE_TIME_TYPES = "CREATE TABLE dateTimeTypes (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  dateColumn DATE NOT NULL,\n" +
            "  timeColumn TIME NOT NULL,\n" +
            "  timeWithoutTimeZoneColumn TIME WITHOUT TIME ZONE NOT NULL,\n" +
            "  timeWithTimeZoneColumn TIME WITH TIME ZONE NOT NULL,\n" +
            "  timestampColumn TIMESTAMP NOT NULL,\n" +
            "  timestampWithoutTimeZone TIMESTAMP WITHOUT TIME ZONE NOT NULL,\n" +
            "  timestampWithTimeZone TIMESTAMP WITH TIME ZONE NOT NULL\n" +
            ");";

    private static final String CREATE_TABLE_JSON_TYPE = "CREATE TABLE jsonType (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  jsonColumn JSON NOT NULL,\n" +
            "  jsonWithLengthColumn JSON(10) NOT NULL\n" +
            ");";

    private static final String CREATE_TABLE_UUID_TYPE = "CREATE TABLE uuidType (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  uuidColumn UUID NOT NULL\n" +
            ");";

    private static final String CREATE_TABLE_JAVA_OBJECT_TYPES = "CREATE TABLE javaObjectTypes (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  javaObjectColumn JAVA_OBJECT NOT NULL,\n" +
            "  objectColumn OBJECT NOT NULL,\n" +
            "  otherColumn OTHER NOT NULL,\n" +
            "  javaObjectWithLengthColumn JAVA_OBJECT(10000) NOT NULL\n" +
            ");";

    private static final String CREATE_TABLE_INTERVAL_TYPES = "CREATE TABLE intervalTypes (\n" +
            "  dummyColumn INTEGER NOT NULL\n" +
//            "  intervalYearColumn INTERVAL YEAR NOT NULL,\n" +
//            "  intervalMonthColumn INTERVAL MONTH NOT NULL,\n" +
//            "  intervalDayColumn INTERVAL DAY NOT NULL,\n" +
//            "  intervalHourColumn INTERVAL HOUR NOT NULL,\n" +
//            "  intervalMinuteColumn INTERVAL MINUTE NOT NULL,\n" +
//            "  intervalSecondColumn INTERVAL SECOND NOT NULL,\n" +
//            "  intervalYearToMonthColumn INTERVAL YEAR TO MONTH NOT NULL,\n" +
//            "  intervalDayToHourColumn INTERVAL DAY TO HOUR NOT NULL,\n" +
//            "  intervalDayToMinuteColumn INTERVAL DAY TO MINUTE NOT NULL,\n" +
//            "  intervalDayToSecondColumn INTERVAL DAY TO SECOND NOT NULL,\n" +
//            "  intervalHourToMinuteColumn INTERVAL HOUR TO MINUTE NOT NULL,\n" +
//            "  intervalHourToSecondColumn INTERVAL HOUR TO SECOND NOT NULL,\n" +
//            "  intervalMinuteToSecondColumn INTERVAL MINUTE TO SECOND NOT NULL\n" +
            ");";

    private static final String CREATE_TABLE_GEOMETRY_TYPES = "CREATE TABLE geometryTypes (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  pointColumn GEOMETRY(POINT) NOT NULL,\n" +
            "  multipointColumn GEOMETRY(MULTIPOINT) NOT NULL,\n" +
            "  linestring GEOMETRY(LINESTRING) NOT NULL,\n" +
            "  multilinestringColumn GEOMETRY(MULTILINESTRING) NOT NULL,\n" +
            "  polygonColumn GEOMETRY(POLYGON) NOT NULL,\n" +
            "  multipolygonColumn GEOMETRY(MULTIPOLYGON) NOT NULL,\n" +
            "  geometryColumn GEOMETRY(GEOMETRY) NOT NULL,\n" +
            "  geometryCollectionColumn GEOMETRY(GEOMETRYCOLLECTION) NOT NULL\n" +
            //"  pointzColumn GEOMETRY(POINT Z) NOT NULL\n" +
            //"  pointmColumn GEOMETRY(POINT M) NOT NULL,\n" +
            //"  pointzmColumn GEOMETRY(POINT ZM) NOT NULL,\n" +
            //"  pointSRIColumn GEOMETRY(POINT, 12) NOT NULL,\n" +
            ");";


    private static final String CREATE_TYPE_AS_ENUM = "CREATE TYPE cardsuit as ENUM ('clubs', 'diamonds', 'hearts', 'spades');\n"
            + "CREATE TABLE createTypeAsEnumTable (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  cardsuitColumn cardsuit NOT NULL\n" +
            ");";

    private static final String CREATE_TABLE_ENUM_TYPE = "CREATE TABLE enumType (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  enumColumn ENUM('clubs', 'diamonds', 'hearts', 'spades') NOT NULL\n" +
            ");";

    private static final String CREATE_TABLE_ARRAY_TYPES = "CREATE TABLE arrayTypes (\n" +
            "  dummyColumn INTEGER NOT NULL,\n" +
            "  integerArrayColumn INTEGER ARRAY NOT NULL,\n" +
            "  booleanArrayColumn BOOLEAN ARRAY NOT NULL,\n" +
            "  varcharArrayColumn VARCHAR ARRAY NOT NULL,\n" +
            "  integerArrayWithMaxLengthColumn INTEGER ARRAY[10] NOT NULL,\n" +
            "  varcharArrayWithMaxLengthColumn VARCHAR ARRAY[10] NOT NULL,\n" +
            "  multidimensionalVarcharArrayColumn VARCHAR ARRAY ARRAY NOT NULL,\n" +
            "  multidimensionalIntegerArrayColumn INTEGER ARRAY ARRAY ARRAY NOT NULL\n" +
            //"  varcharWithSizeArrayColumn VARCHAR(10) ARRAY NOT NULL\n" +
            //"  multidimensionalArrayWithMaxLengths BOOLEAN ARRAY[5] ARRAY[6] ARRAY[3] NOT NULL,\n" +
            ");";

    private static final String CREATE_TABLES_SQL = CREATE_TABLE_CHARACTER_TYPES
            + CREATE_TABLE_CHARACTER_VARYING_TYPES
            + CREATE_TABLE_CHARACTER_LARGE_OBJECT_TYPES
            + CREATE_TABLE_VARCHAR_IGNORE_CASE
            + CREATE_TABLE_BINARY_TYPES
            + CREATE_TABLE_NUMERIC_TYPES
            + CREATE_TABLE_DATE_TIME_TYPES
            + CREATE_TABLE_JSON_TYPE
            + CREATE_TABLE_UUID_TYPE
            + CREATE_TABLE_INTERVAL_TYPES
            + CREATE_TABLE_JAVA_OBJECT_TYPES
            + CREATE_TABLE_GEOMETRY_TYPES
            + CREATE_TABLE_ENUM_TYPE
            + CREATE_TABLE_ARRAY_TYPES
            +CREATE_TYPE_AS_ENUM;

    public H2ColumnTypesController() {
        this(H2ColumnTypesApplication.class);
    }

    public static void main(String[] args) {
        H2ColumnTypesController controller = new H2ColumnTypesController();
        controller.setControllerPort(40100);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);
        starter.start();
    }

    static {
        /*
         * To avoid issues with non-determinism checks (in particular in the handling of taint-analysis),
         * we must disable the cache in H2
         */
        System.setProperty("h2.objectCache", "false");
    }

    protected Connection sqlConnection;

    protected H2ColumnTypesController(Class<?> applicationClass) {
        super(applicationClass);
    }


    @Override
    public String startSut() {


        //lot of problem if using same H2 instance. see:
        //https://github.com/h2database/h2database/issues/227
        int rand = Random.Default.nextInt();

        ctx = SpringApplication.run(applicationClass, "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:testdb_" + rand + ";DB_CLOSE_DELAY=-1;",
                "--spring.jpa.database-platform=" + H2Dialect.class.getName(),
                "--spring.datasource.username=sa",
                "--spring.datasource.password",
                "--spring.jpa.properties.hibernate.show_sql=true");


        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);

        try {
            sqlConnection = jdbc.getDataSource().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        // execute create table
        try {
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return "http://localhost:" + getSutPort();
    }

    private void createTables() throws SQLException {
        PreparedStatement stmt = sqlConnection.prepareStatement(CREATE_TABLES_SQL);
        stmt.execute();
    }

    @Override
    public void resetStateOfSUT() {

    }

    @Override
    public void stopSut() {
        super.stopSut();
//        sqlConnection = null;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return Collections.singletonList(new DbSpecification(DatabaseType.H2, sqlConnection));
    }


}
