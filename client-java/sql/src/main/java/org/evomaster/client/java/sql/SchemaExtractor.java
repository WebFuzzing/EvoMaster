package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.*;
import org.evomaster.client.java.sql.internal.constraint.*;
import org.evomaster.client.java.utils.SimpleLogger;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SchemaExtractor {


    public static final String GEOMETRY = "GEOMETRY";

    public static boolean validate(DbSchemaDto schema) throws IllegalArgumentException {

        /*
            some checks if the derived schema is consistent
         */

        Objects.requireNonNull(schema);

        for (TableDto table : schema.tables) {

            for (ColumnDto column : table.columns) {
                checkForeignKeyToAutoIncrementPresent(schema, table, column);
                checkForeignKeyToAutoIncrementMissing(schema, table, column);
                checkEnumeratedTypeIsDefined(schema, table, column);
            }

        }

        return true;
    }

    private static void checkEnumeratedTypeIsDefined(DbSchemaDto schema, TableDto table, ColumnDto column) {
        if (column.isEnumeratedType) {
            if (schema.enumeraredTypes.stream().noneMatch(k -> k.name.equals(column.type))) {
                throw new IllegalArgumentException("Missing enumerated type declaration for type " + column.type
                        + " in column " + column.name
                        + " of table " + table.name);
            }
        }
    }

    private static void checkForeignKeyToAutoIncrementMissing(DbSchemaDto schema, TableDto table, ColumnDto column) {
        if (column.foreignKeyToAutoIncrement) {
            return;
        }

        Optional<ForeignKeyDto> fk = table.foreignKeys.stream()
                .filter(it -> it.sourceColumns.contains(column.name))
                .findFirst();

        if (!fk.isPresent()) {
            //not a foreign key
            return;
        }

        //TODO proper handling of multi-column PKs/FKs

        Optional<TableDto> targetTable = schema.tables.stream()
                .filter(t -> t.name.equals(fk.get().targetTable))
                .findFirst();

        if (!targetTable.isPresent()) {
            throw new IllegalArgumentException("Foreign key in table " + table.name +
                    " pointing to non-existent table " + fk.get().targetTable);
        }

        List<ColumnDto> pks = targetTable.get().columns.stream()
                .filter(c -> c.primaryKey)
                .collect(Collectors.toList());

        if (pks.isEmpty()) {
            throw new IllegalArgumentException("No PK in table " + targetTable.get().name + " that has FKs pointing to it");
        }

        for (ColumnDto pk : pks) {
            if (pk.autoIncrement || pk.foreignKeyToAutoIncrement) {
                throw new IllegalArgumentException("Column " + pk.name + " in table " +
                        pk.table + " is auto-increment, although FK pointing to it does not mark it " +
                        "as autoincrement in " + column.name + " in " + table.name
                );
            }
        }
    }

    private static void checkForeignKeyToAutoIncrementPresent(DbSchemaDto schema, TableDto table, ColumnDto column) {
        if (!column.foreignKeyToAutoIncrement) {
            return;
        }

        Optional<ForeignKeyDto> fk = table.foreignKeys.stream()
                .filter(it -> it.sourceColumns.contains(column.name))
                .findFirst();

        if (!fk.isPresent()) {
            throw new IllegalArgumentException("No foreign key constraint for marked column " +
                    column.name + " in table " + table.name);
        }

        //TODO proper handling of multi-column PKs/FKs

        Optional<TableDto> targetTable = schema.tables.stream()
                .filter(t -> t.name.equals(fk.get().targetTable))
                .findFirst();

        if (!targetTable.isPresent()) {
            throw new IllegalArgumentException("Foreign key in table " + table.name +
                    " pointing to non-existent table " + fk.get().targetTable);
        }

        //there should be only 1 PK, and that must be auto-increment

        List<ColumnDto> pks = targetTable.get().columns.stream()
                .filter(c -> c.primaryKey)
                .collect(Collectors.toList());

        if (pks.size() != 1) {
            throw new IllegalArgumentException("There must be only 1 PK in table " +
                    targetTable.get().name + " pointed by the FK-to-autoincrement " +
                    column.name + " in " + table.name + ". However, there were: " + pks.size());
        }

        ColumnDto pk = pks.get(0);
        if (!pk.autoIncrement && !pk.foreignKeyToAutoIncrement) {
            throw new IllegalArgumentException("Column " + pk.name + " in table " +
                    pk.table + " is not auto-increment, although FK pointing to it does mark it" +
                    "as autoincrement in " + column.name + " in " + table.name
            );
        }
    }

    public static DbSchemaDto extract(Connection connection) throws Exception {

        Objects.requireNonNull(connection);

        DbSchemaDto schemaDto = new DbSchemaDto();

        DatabaseMetaData md = connection.getMetaData();

        String protocol = md.getURL(); //TODO better handling
        DatabaseType dt = DatabaseType.OTHER;
        if (protocol.contains(":h2")) {
            dt = DatabaseType.H2;
        } else if (protocol.contains(":derby")) {
            dt = DatabaseType.DERBY;
        } else if (protocol.contains(":postgresql")) {
            dt = DatabaseType.POSTGRES;
        } else if (protocol.contains(":mysql")) {
            //https://dev.mysql.com/doc/refman/8.0/en/connecting-using-uri-or-key-value-pairs.html#connecting-using-uri
            dt = DatabaseType.MYSQL;
        }
        schemaDto.databaseType = dt;


        /*
            schema name
         */
        schemaDto.name = getSchemaName(connection, dt);

        if (dt.equals(DatabaseType.POSTGRES)) {
            Map<String, Set<String>> enumLabels = getPostgresEnumTypes(connection);
            addPostgresEnumTypesToSchema(schemaDto, enumLabels);
            schemaDto.compositeTypes = getPostgresCompositeTypes(connection);
        }

        ResultSet tables = md.getTables(null, schemaDto.name, null, new String[]{"TABLE"});

        Set<String> tableNames = new HashSet<>();

        /*
            Interfaces to deal with DBs are simply awful...
            Here, we first check with schema name in upper case, and, if that gives no results,
            we try with lower case... this is because different databases deal with upper/lower
            cases differently.
            But API does not give you any info on whether result set
            is empty or not, and only way is to call next()
         */
        if (!tables.next()) {
            tables.close();
            schemaDto.name = schemaDto.name.toLowerCase();
            tables = md.getTables(null, schemaDto.name, null, new String[]{"TABLE"});
            if (tables.next()) {
                do {
                    handleTableEntry(connection, schemaDto, md, tables, tableNames);
                } while (tables.next());
            }
        } else {
            do {
                handleTableEntry(connection, schemaDto, md, tables, tableNames);
            } while (tables.next());
        }
        tables.close();

        /*
            Mark those columns that are using auto generated values
         */
        addForeignKeyToAutoIncrement(schemaDto);

        /*
            JDBC MetaData is quite limited.
            To check constraints, we need to do SQL queries on the system tables.
            Unfortunately, this is database-dependent
         */
        addConstraints(connection, dt, schemaDto);

        if (dt.equals(DatabaseType.POSTGRES)) {
            List<ColumnAttributes> columnAttributes = getPostgresColumnAttributes(connection);
            addColumnAttributes(schemaDto, columnAttributes);
        } else if (dt.equals(DatabaseType.H2)) {
            List<DbTableConstraint> h2EnumConstraints = getH2EnumTypes(schemaDto.name, md);
            addConstraints(schemaDto, h2EnumConstraints);
        }

        assert validate(schemaDto);

        return schemaDto;
    }

    private static void addColumnAttributes(DbSchemaDto schemaDto, List<ColumnAttributes> listOfColumnAttributes) {
        for (ColumnAttributes columnAttributes : listOfColumnAttributes) {
            String tableName = columnAttributes.tableName;
            String columnName = columnAttributes.columnName;
            ColumnDto columnDto = getColumnDto(schemaDto, tableName, columnName);
            columnDto.numberOfDimensions = columnAttributes.numberOfDimensions;
        }
    }

    private static ColumnDto getColumnDto(DbSchemaDto schemaDto, String tableName, String columnName) {
        TableDto tableDto = schemaDto.tables.stream()
                .filter(t -> t.name.equals(tableName.toLowerCase()))
                .findFirst()
                .orElse(null);
        return tableDto.columns.stream()
                .filter(c -> c.name.equals(columnName.toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    private static String getSchemaName(Connection connection, DatabaseType dt) throws SQLException {
        String schemaName;
        if (dt.equals(DatabaseType.MYSQL)) {

            // schema is database name in mysql
            schemaName = connection.getCatalog();
        } else {
            try {
                schemaName = connection.getSchema();
            } catch (Exception | AbstractMethodError e) {
                /*
                    In remote sessions, getSchema might fail.
                    We do not do much with it anyway (at least for
                    now), so not a big deal...
                    Furthermore, some drivers might be compiled to Java 6,
                    whereas getSchema was introduced in Java 7
                 */
                schemaName = "public";
            }

            //see https://www.progress.com/blogs/jdbc-tutorial-extracting-database-metadata-via-jdbc-driver
            schemaName = schemaName.toUpperCase();
        }
        return schemaName;
    }

    private static class ColumnAttributes {
        public String tableName;
        public String columnName;
        public int numberOfDimensions;
    }

    private static List<ColumnAttributes> getPostgresColumnAttributes(Connection connection) throws SQLException {
        String query = "SELECT pg_namespace.nspname as TABLE_NAMESPACE, pg_class.relname as TABLE_NAME, pg_attribute.attname as COLUMN_NAME, pg_attribute.attndims as NUMBER_OF_DIMENSIONS \n" +
                "FROM pg_attribute \n" +
                "INNER JOIN pg_class ON pg_class.oid = pg_attribute.attrelid " +
                "INNER JOIN pg_namespace ON pg_namespace.oid = pg_class.relnamespace " +
                "WHERE pg_namespace.nspname != 'pg_catalog' ";

        List<ColumnAttributes> listOfColumnAttributes = new LinkedList<>();
        try (Statement stmt = connection.createStatement()) {
            ResultSet columnAttributesResultSet = stmt.executeQuery(query);
            while (columnAttributesResultSet.next()) {
                String tableNamesapce = columnAttributesResultSet.getString("TABLE_NAMESPACE");
                String tableName = columnAttributesResultSet.getString("TABLE_NAME");
                String columnName = columnAttributesResultSet.getString("COLUMN_NAME");
                int numberOfDimensions = columnAttributesResultSet.getInt("NUMBER_OF_DIMENSIONS");

                if (numberOfDimensions == 0) {
                    // skip attribute rows when data types are not arrays, matrixes, etc.
                    continue;
                }

                ColumnAttributes columnAttributes = new ColumnAttributes();
                columnAttributes.tableName = tableName;
                columnAttributes.columnName = columnName;
                columnAttributes.numberOfDimensions = numberOfDimensions;

                listOfColumnAttributes.add(columnAttributes);
            }
        }
        return listOfColumnAttributes;
    }

    private static List<String> getAllCompositeTypeNames(Connection connection) throws SQLException {
        // Source: https://stackoverflow.com/questions/3660787/how-to-list-custom-types-using-postgres-information-schema
        String listAllCompositeTypesQuery = "SELECT      n.nspname as schema, t.typname as typename \n" +
                "FROM        pg_type t \n" +
                "LEFT JOIN   pg_catalog.pg_namespace n ON n.oid = t.typnamespace \n" +
                "WHERE       (t.typrelid = 0 OR (SELECT c.relkind = 'c' FROM pg_catalog.pg_class c WHERE c.oid = t.typrelid)) \n" +
                "AND     NOT EXISTS(SELECT 1 FROM pg_catalog.pg_type el WHERE el.oid = t.typelem AND el.typarray = t.oid)\n" +
                "AND     n.nspname NOT IN ('pg_catalog', 'information_schema')" +
                "AND     t.typtype ='c';";

        List<String> compositeTypeNames = new ArrayList<>();
        try (Statement listAllCompositeTypesStmt = connection.createStatement()) {
            ResultSet listAllCompositeTypesResultSet = listAllCompositeTypesStmt.executeQuery(listAllCompositeTypesQuery);
            while (listAllCompositeTypesResultSet.next()) {
                compositeTypeNames.add(listAllCompositeTypesResultSet.getString("typename"));
            }
        }
        return compositeTypeNames;
    }

    private static List<CompositeTypeDto> getPostgresCompositeTypes(Connection connection) throws SQLException {
        List<CompositeTypeDto> compositeTypeDtos = new ArrayList<>();
        List<String> compositeTypeNames = getAllCompositeTypeNames(connection);
        for (String compositeTypeName : compositeTypeNames) {
            List<CompositeTypeColumnDto> columnDtos = getAllCompositeTypeColumns(connection, compositeTypeName, compositeTypeNames);
            CompositeTypeDto compositeTypeDto = new CompositeTypeDto();
            compositeTypeDto.name = compositeTypeName;
            compositeTypeDto.columns = columnDtos;
            compositeTypeDtos.add(compositeTypeDto);

        }
        return compositeTypeDtos;
    }

    private static final String TEXT_DATA_TYPE = "text";

    private static final String BIT_DATA_TYPE = "bit";

    private static final String VAR_BIT_DATA_TYPE = "varbit";

    private static final String CHAR_DATA_TYPE = "char";

    private static final String VAR_CHAR_DATA_TYPE = "varchar";

    private static final String BLANK_PADDED_CHAR_DATA_TYPE = "bpchar";

    private static final String NUMERIC_DATA_TYPE = "numeric";

    private static List<CompositeTypeColumnDto> getAllCompositeTypeColumns(Connection connection, String compositeTypeName, List<String> allCompositeTypeNames) throws SQLException {
        // Source: https://stackoverflow.com/questions/6979282/postgresql-find-information-about-user-defined-types
        String listAttributesQuery = String.format(
                "SELECT pg_attribute.attname AS attname, pg_attribute.attlen  as attlen, pg_type.typname AS typename " +
                        " FROM pg_attribute " +
                        " JOIN pg_type ON pg_attribute.atttypid=pg_type.oid " +
                        " WHERE pg_attribute.attrelid =\n" +
                        "  (SELECT typrelid FROM pg_type WHERE typname = '%s') " +
                        " ORDER BY pg_attribute.attnum ", compositeTypeName);

        List<CompositeTypeColumnDto> columnDtos = new ArrayList<>();
        try (Statement listAttributesStmt = connection.createStatement()) {
            ResultSet listAttributesResultSet = listAttributesStmt.executeQuery(listAttributesQuery);
            while (listAttributesResultSet.next()) {
                CompositeTypeColumnDto columnDto = new CompositeTypeColumnDto();
                columnDto.name = listAttributesResultSet.getString("attname");
                columnDto.type = listAttributesResultSet.getString("typename");
                int attlen = listAttributesResultSet.getInt("attlen");
                /*
                 *  Composite type columns can not include constraints (such as NOT NULL).
                 *  Therefore, all columns in composite types are nullable.
                 */
                columnDto.nullable = true;
                columnDto.columnTypeIsComposite = allCompositeTypeNames.stream().anyMatch(t -> t.equalsIgnoreCase(columnDto.type));

                if (columnDto.columnTypeIsComposite) {
                    columnDto.size = 0;
                } else {
                    switch (columnDto.type) {
                        case TEXT_DATA_TYPE: {
                            columnDto.size = Integer.MAX_VALUE;
                        }
                        break;
                        case NUMERIC_DATA_TYPE:
                        case CHAR_DATA_TYPE:
                        case VAR_CHAR_DATA_TYPE:
                        case BIT_DATA_TYPE:
                        case VAR_BIT_DATA_TYPE:
                        case BLANK_PADDED_CHAR_DATA_TYPE: {
                            throw new UnsupportedOperationException("cannot get variable length size of type (varchar, char, varbit, bit, numeric) currently not supported for postgres composite types: " + columnDto.name + " with type " + columnDto.type);
                        }
                        default: {
                            columnDto.size = attlen;
                        }
                    }
                }

                columnDtos.add(columnDto);
            }
        }
        return columnDtos;
    }

    /**
     * In H2, the ENUM columns are stored as the column type.
     * We return the definition of each enum column using TABLE_COLUMN -> [ENUM VALUES]
     *
     * @param md the database metadata
     * @return a list of enum constraints
     * @throws SQLException if any column name is incorrect
     */
    private static List<DbTableConstraint> getH2EnumTypes(String schemaName, DatabaseMetaData md) throws SQLException {
        List<DbTableConstraint> enumTypesConstraints = new LinkedList<>();
        ResultSet tables = md.getTables(null, schemaName, null, new String[]{"TABLE"});
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            ResultSet columns = md.getColumns(null, schemaName, tableName, null);
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String typeName = columns.getString("TYPE_NAME");
                if (typeName.startsWith("ENUM")) {
                    String sqlExpression = String.format("(\"%s\" IN %s)", columnName, typeName.substring("ENUM".length()));
                    DbTableCheckExpression constraint = new DbTableCheckExpression(tableName, sqlExpression);
                    enumTypesConstraints.add(constraint);
                }
            }
        }
        return enumTypesConstraints;
    }

    private static Map<String, Set<String>> getPostgresEnumTypes(Connection connection) throws SQLException {
        String query = "SELECT t.typname, e.enumlabel\n" +
                "FROM pg_type AS t\n" +
                "   JOIN pg_enum AS e ON t.oid = e.enumtypid\n" +
                "ORDER BY e.enumsortorder;";
        Map<String, Set<String>> enumLabels = new LinkedHashMap<>();
        try (Statement stmt = connection.createStatement()) {
            ResultSet enumTypeValues = stmt.executeQuery(query);
            while (enumTypeValues.next()) {
                String typeName = enumTypeValues.getString("typname");
                String enumLabel = enumTypeValues.getString("enumlabel");
                if (!enumLabels.containsKey(typeName)) {
                    enumLabels.put(typeName, new HashSet<>());
                }
                enumLabels.get(typeName).add(enumLabel);
            }
        }
        return enumLabels;
    }

    private static void addPostgresEnumTypesToSchema(DbSchemaDto schemaDto, Map<String, Set<String>> enumLabels) {
        enumLabels.forEach(
                (k, v) -> {
                    EnumeratedTypeDto enumeratedTypeDto = new EnumeratedTypeDto();
                    enumeratedTypeDto.name = k;
                    enumeratedTypeDto.values = new ArrayList<>(v);
                    schemaDto.enumeraredTypes.add(enumeratedTypeDto);
                }
        );
    }


    /**
     * Adds a unique constraint to the corresponding ColumnDTO for the selected table.column pair.
     * Requires the ColumnDTO to be contained in the TableDTO.
     * If the column DTO is not contained, a IllegalArgumentException is thrown.
     *
     * @param tableDto   the DTO of the table
     * @param columnName the name of the column to add the unique constraint on
     **/
    public static void addUniqueConstraintToColumn(TableDto tableDto, String columnName) {

        ColumnDto columnDto = tableDto.columns.stream()
                .filter(c -> c.name.equals(columnName)).findAny().orElse(null);

        if (columnDto == null) {
            throw new IllegalArgumentException("Missing column DTO for column:" + tableDto.name + "." + columnName);
        }

        columnDto.unique = true;
    }

    /**
     * Appends constraints that are database specific.
     */
    private static void addConstraints(Connection connection, DatabaseType dt, DbSchemaDto schemaDto) throws SQLException {
        TableConstraintExtractor constraintExtractor = TableConstraintExtractorFactory.buildConstraintExtractor(dt);
        if (constraintExtractor != null) {
            final List<DbTableConstraint> dbTableConstraints = constraintExtractor.extract(connection, schemaDto);
            addConstraints(schemaDto, dbTableConstraints);
        } else {
            SimpleLogger.uniqueWarn("WARNING: EvoMaster cannot extract constraints from database " + dt);
        }

    }

    private static void addConstraints(DbSchemaDto schemaDto, List<DbTableConstraint> constraintList) {
        for (DbTableConstraint constraint : constraintList) {
            String tableName = constraint.getTableName();
            TableDto tableDto = schemaDto.tables.stream().filter(t -> t.name.equalsIgnoreCase(tableName)).findFirst().orElse(null);

            if (tableDto == null) {
                throw new NullPointerException("TableDto for table " + tableName + " was not found in the schemaDto");
            }

            if (constraint instanceof DbTableCheckExpression) {
                TableCheckExpressionDto constraintDto = new TableCheckExpressionDto();
                final DbTableCheckExpression tableCheckExpression = (DbTableCheckExpression) constraint;
                constraintDto.sqlCheckExpression = tableCheckExpression.getSqlCheckExpression();
                tableDto.tableCheckExpressions.add(constraintDto);
            } else if (constraint instanceof DbTableUniqueConstraint) {
                DbTableUniqueConstraint tableUniqueConstraint = (DbTableUniqueConstraint) constraint;
                for (String columnName : tableUniqueConstraint.getUniqueColumnNames()) {
                    addUniqueConstraintToColumn(tableDto, columnName);
                }
            } else {
                throw new RuntimeException("Unknown constraint type " + constraint.getClass().getName());
            }

        }
    }

    private static void handleTableEntry(Connection connection, DbSchemaDto schemaDto, DatabaseMetaData md, ResultSet tables, Set<String> tableNames) throws SQLException {
        TableDto tableDto = new TableDto();
        schemaDto.tables.add(tableDto);
        tableDto.name = tables.getString("TABLE_NAME");

        if (tableNames.contains(tableDto.name)) {
            /*
             * Perhaps we should throw a more specific exception than IllegalArgumentException
             */
            throw new IllegalArgumentException("Cannot handle repeated table " + tableDto.name + " in schema");
        } else {
            tableNames.add(tableDto.name);
        }

        Set<String> pks = new HashSet<>();
        SortedMap<Integer, String> primaryKeySequence = new TreeMap<>();
        ResultSet rsPK = md.getPrimaryKeys(null, null, tableDto.name);


        while (rsPK.next()) {
            String pkColumnName = rsPK.getString("COLUMN_NAME");
            int positionInPrimaryKey = rsPK.getShort("KEY_SEQ");
            pks.add(pkColumnName);
            int pkIndex = positionInPrimaryKey - 1;
            primaryKeySequence.put(pkIndex, pkColumnName);
        }
        rsPK.close();

        tableDto.primaryKeySequence.addAll(primaryKeySequence.values());

        ResultSet columns = md.getColumns(null, schemaDto.name, tableDto.name, null);

        Set<String> columnNames = new HashSet<>();
        while (columns.next()) {

            ColumnDto columnDto = new ColumnDto();
            tableDto.columns.add(columnDto);

            columnDto.table = tableDto.name;
            columnDto.name = columns.getString("COLUMN_NAME");

            if (columnNames.contains(columnDto.name)) {
                /*
                 * Perhaps we should throw a more specific exception than IllegalArgumentException
                 */
                throw new IllegalArgumentException("Cannot handle repeated column " + columnDto.name + " in table " + tableDto.name);
            } else {
                columnNames.add(columnDto.name);
            }


            String typeAsString = columns.getString("TYPE_NAME");
            columnDto.size = columns.getInt("COLUMN_SIZE");

            switch (schemaDto.databaseType) {
                case MYSQL:
                    extractMySQLColumn(schemaDto, tableDto, columnDto, typeAsString, columns, connection);
                    break;
                case POSTGRES:
                    extractPostgresColumn(schemaDto, columnDto, typeAsString, columns);
                    break;
                case H2:
                    extractH2Column(columnDto, typeAsString, columns);
                    break;

                default:
                    columnDto.nullable = columns.getBoolean("IS_NULLABLE");
                    columnDto.autoIncrement = columns.getBoolean("IS_AUTOINCREMENT");

                    // might need to support unsigned property of numeric in other types of db
                    columnDto.type = typeAsString;
                    // TODO handle precision for other databases
            }
            //columns.getString("DECIMAL_DIGITS");

            columnDto.primaryKey = pks.contains(columnDto.name);
        }
        columns.close();


        ResultSet fks = md.getImportedKeys(null, null, tableDto.name);
        while (fks.next()) {
            //TODO need to see how to handle case of multi-columns

            ForeignKeyDto fkDto = new ForeignKeyDto();
            fkDto.sourceColumns.add(fks.getString("FKCOLUMN_NAME"));
            fkDto.targetTable = fks.getString("PKTABLE_NAME");

            tableDto.foreignKeys.add(fkDto);
        }
        fks.close();
    }

    private static void extractH2Column(ColumnDto columnDto, String typeAsString, ResultSet columns) throws SQLException {
        columnDto.nullable = columns.getBoolean("IS_NULLABLE");
        columnDto.autoIncrement = columns.getBoolean("IS_AUTOINCREMENT");
        /*
         * In H2, ENUM types are always VARCHAR Columns
         */
        if (typeAsString.startsWith("ENUM")) {
            columnDto.type = "VARCHAR";
        } else
            /*
             * In H2, there is no other way of obtaining
             * the number of dimensionas for arrays/multi
             * dimensional arrays except parsing the
             * type string.
             */
            if (typeAsString.contains("ARRAY")) {
                columnDto.type = getH2ArrayBaseType(typeAsString);
                columnDto.numberOfDimensions = getH2ArrayNumberOfDimensions(typeAsString);
            } else {
                columnDto.type = typeAsString;
            }


    }

    private static void extractPostgresColumn(DbSchemaDto schemaDto,
                                              ColumnDto columnDto,
                                              String typeAsString,
                                              ResultSet columns) throws SQLException {
        columnDto.nullable = columns.getBoolean("IS_NULLABLE");
        columnDto.autoIncrement = columns.getBoolean("IS_AUTOINCREMENT");
        columnDto.type = typeAsString;
        columnDto.isEnumeratedType = schemaDto.enumeraredTypes.stream()
                .anyMatch(k -> k.name.equals(typeAsString));
        columnDto.isCompositeType = schemaDto.compositeTypes.stream()
                .anyMatch(k -> k.name.equals(typeAsString));
    }

    private static void extractMySQLColumn(DbSchemaDto schemaDto,
                                           TableDto tableDto,
                                           ColumnDto columnDto,
                                           String typeAsStringValue,
                                           ResultSet columns,
                                           Connection connection) throws SQLException {

        int decimalDigitsValue = columns.getInt("DECIMAL_DIGITS");
        int nullableValue = columns.getInt("NULLABLE");
        String isAutoIncrementValue = columns.getString("IS_AUTOINCREMENT");

        // numeric https://dev.mysql.com/doc/refman/8.0/en/numeric-type-syntax.html
        String[] attrs = typeAsStringValue.split(" ");
        if (attrs.length == 0)
            throw new IllegalStateException("missing type info of the column");

        if (attrs[0].equalsIgnoreCase(GEOMETRY)) {
            /*
             * In MYSQL, the TYPE_NAME column of the JDBC table metadata returns the GEOMETRY data type,
             * which is the supertype of all geometry data. In order to know the specific geometric data
             * type of a column, it is required to query the [INFORMATION_SCHEMA.COLUMNS] table for the
             * corresponding [DATA_TYPE] column value.
             */
            String sqlQuery = String.format("SELECT DATA_TYPE, table_schema from INFORMATION_SCHEMA.COLUMNS where\n" +
                    " table_schema = '%s' and table_name = '%s' and column_name= '%s' ", schemaDto.name, tableDto.name, columnDto.name);
            try (Statement statement = connection.createStatement()) {
                ResultSet rs = statement.executeQuery(sqlQuery);
                if (rs.next()) {
                    String dataType = rs.getString("DATA_TYPE");
                    /*
                     * uppercase to enforce case insensitivity.
                     */
                    columnDto.type = dataType.toUpperCase();
                } else {
                    columnDto.type = GEOMETRY;
                }
            }
        } else {
            columnDto.type = attrs[0];
        }

        columnDto.isUnsigned = attrs.length > 1 && IntStream
                .range(1, attrs.length).anyMatch(i -> attrs[i].equalsIgnoreCase("UNSIGNED"));
        columnDto.nullable = nullableValue == DatabaseMetaData.columnNullable;
        columnDto.autoIncrement = isAutoIncrementValue.equalsIgnoreCase("yes");
                    /*
                        this precision is only used for decimal, not for double and float in mysql
                        https://dev.mysql.com/doc/refman/8.0/en/floating-point-types.html
                        therefore, here, we only set precision when type is DECIMAL
                     */
        if (columnDto.type.equals("DECIMAL")) {
            columnDto.scale = decimalDigitsValue;
            // default is 0
            if (columnDto.scale < 0)
                columnDto.scale = 0;
        }
    }

    private static int getH2ArrayNumberOfDimensions(String typeAsString) {
        if (!typeAsString.contains("ARRAY")) {
            throw new IllegalArgumentException("Cannot get number of dimensions of non-array type " + typeAsString);
        }
        Pattern arrayOnlyPattern = Pattern.compile("ARRAY");
        Matcher arrayOnlyMatcher = arrayOnlyPattern.matcher(typeAsString);
        int numberOfDimensions = 0;
        while (arrayOnlyMatcher.find()) {
            numberOfDimensions++;
        }
        return numberOfDimensions;
    }

    private static String getH2ArrayBaseType(String typeAsString) {
        if (!typeAsString.contains("ARRAY")) {
            throw new IllegalArgumentException("Cannot get base type from non-array type " + typeAsString);
        }
        Pattern pattern = Pattern.compile("\\s*ARRAY\\s*\\[\\s*\\d+\\s*\\]");
        Matcher matcher = pattern.matcher(typeAsString);
        if (matcher.find()) {
            throw new IllegalArgumentException("Cannot handle array type with maximum length " + typeAsString);
        }
        /*
         * The typeAsString does have ARRAY but it does not have maximum length
         * (it is still not supported).
         */
        String baseType = typeAsString.replaceAll("ARRAY", "").trim();
        return baseType.trim();
    }

    /**
     * Sets the foreignKeyToAutoIncrement field in the Column DTO
     * when a column is a foreign key to an auto increment value.
     * This information will be needed to properly handle the
     * automatically generated values in primary keys that are
     * referenced by columns in other tables
     *
     * @param schema a DTO with the database information
     */
    private static void addForeignKeyToAutoIncrement(DbSchemaDto schema) {
        for (TableDto tableDto : schema.tables) {
            for (ColumnDto columnDto : tableDto.columns) {
                if (isFKToAutoIncrementColumn(schema, tableDto, columnDto.name)) {
                    columnDto.foreignKeyToAutoIncrement = true;
                }
            }
        }
    }

    /**
     * @return a table DTO for a particular table name
     */
    private static TableDto getTable(DbSchemaDto schema, String tableName) {
        return schema.tables.stream()
                .filter(t -> t.name.equalsIgnoreCase(tableName))
                .findFirst().orElse(null);
    }

    private static ColumnDto getColumn(TableDto table, String columnName) {
        return table.columns.stream()
                .filter(c -> c.name.equalsIgnoreCase(columnName))
                .findFirst().orElse(null);
    }


    /**
     * Checks if the given table/column is a foreign key to an autoincrement column.
     * This is done to be able to compute foreignKeyToAutoIncrement boolean.
     * Otherwise, we could just read that boolean.
     *
     * @return true if the given table/column is a foreign key to an autoincrement column.
     */
    private static boolean isFKToAutoIncrementColumn(DbSchemaDto schema, TableDto tableDto, String columnName) {

        Objects.requireNonNull(schema);
        Objects.requireNonNull(tableDto);
        Objects.requireNonNull(columnName);

        // is this column among the declared FKs?
        if (tableDto.foreignKeys.stream()
                .noneMatch(fk -> fk.sourceColumns.stream()
                        .anyMatch(s -> s.equalsIgnoreCase(columnName)))) {
            return false;
        }

        ColumnDto columnDto = getColumn(tableDto, columnName);

        if (columnDto.autoIncrement) {
            // Assuming here that a FK cannot be auto-increment
            return false;
        }

        // check if the column belongs to a foreign key that is non printable
        for (ForeignKeyDto fk : tableDto.foreignKeys) {
            if (fk.sourceColumns.stream()
                    .anyMatch(s -> s.equalsIgnoreCase(columnName))) {

                /*
                    TODO: instead of using those positions, should have proper
                    support for multi-column PKs/FKs
                 */
                int positionInFKSequence = fk.sourceColumns.indexOf(columnName);
                TableDto targetTableDto = getTable(schema, fk.targetTable);
                String targetColumnName = targetTableDto.primaryKeySequence.get(positionInFKSequence);
                ColumnDto targetColumnDto = getColumn(targetTableDto, targetColumnName);

                /*
                    Either that target PK is auto-increment, or itself is a FK to a non-printable PK
                 */
                if (targetColumnDto.autoIncrement ||
                        isFKToAutoIncrementColumn(schema, targetTableDto, targetColumnName)) {
                    return true;
                }
            }
        }

        return false;
    }

}
