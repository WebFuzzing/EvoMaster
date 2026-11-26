package org.evomaster.client.java.controller.internal.db.sql.h2;

import org.evomaster.client.java.controller.internal.SutController;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Verify dependency ordering and repeated seed restoration with a real H2 database. */
class H2SeedRestoreOrderTest {

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> dependencies(SutController controller) throws Exception {
        Field field = SutController.class.getDeclaredField("fkMap");
        field.setAccessible(true);
        return (Map<String, List<String>>) field.get(controller);
    }

    private int depth(SutController controller, String name) throws Exception {
        Method method = SutController.class.getDeclaredMethod("getFkDepth", String.class, Set.class);
        method.setAccessible(true);
        return (int) method.invoke(controller, name, new HashSet<String>());
    }

    @Test
    void qualifiedAndBareSelfReferencesHaveTheSameDepth() throws Exception {
        SutController controller = new DatabaseFakeH2SutController(null);
        Map<String, List<String>> map = dependencies(controller);
        map.put("PUBLIC.LOCATIONS", Collections.emptyList());
        map.put("PUBLIC.DEPARTMENTS", Arrays.asList("LOCATIONS"));
        map.put("PUBLIC.EMPLOYEES", Arrays.asList("DEPARTMENTS", "EMPLOYEES"));
        map.put("PUBLIC.USER_CREDENTIALS", Arrays.asList("EMPLOYEES"));
        assertEquals(2, depth(controller, "PUBLIC.EMPLOYEES"));
        assertEquals(2, depth(controller, "employees"));
        assertEquals(3, depth(controller, "PUBLIC.USER_CREDENTIALS"));
        assertEquals(-1, depth(controller, "PLOYEES"));
    }

    @Test
    void sharedDependenciesDoNotChangeDepthWithTraversalOrder() throws Exception {
        SutController controller = new DatabaseFakeH2SutController(null);
        Map<String, List<String>> map = dependencies(controller);
        map.put("PUBLIC.A", Collections.emptyList());
        map.put("PUBLIC.B", Arrays.asList("A"));
        map.put("PUBLIC.C", Arrays.asList("B"));
        map.put("PUBLIC.D", Arrays.asList("A", "B", "C"));
        assertEquals(3, depth(controller, "D"));
        map.put("PUBLIC.D", Arrays.asList("C", "B", "A"));
        assertEquals(3, depth(controller, "D"));
        map.put("OTHER.A", Arrays.asList("PUBLIC.D"));
        assertEquals(0, depth(controller, "PUBLIC.A"));
        // Explicit schema names are not collapsed into a single table.
        assertEquals(-1, depth(controller, "MISSING.A"));
    }

    @Test
    void actualResetRestoresEmployeesBeforeCredentialsRepeatedly() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:restore_" + UUID.randomUUID(), "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE LOCATIONS(ID INT PRIMARY KEY)");
                statement.execute("CREATE TABLE DEPARTMENTS(ID INT PRIMARY KEY, LOCATION_ID INT REFERENCES LOCATIONS(ID))");
                statement.execute("CREATE TABLE EMPLOYEES(ID INT PRIMARY KEY, DEPARTMENT_ID INT REFERENCES DEPARTMENTS(ID), MANAGER_ID INT REFERENCES EMPLOYEES(ID))");
                statement.execute("CREATE TABLE USER_CREDENTIALS(ID INT PRIMARY KEY, EMPLOYEE_ID INT REFERENCES EMPLOYEES(ID))");
            }
            String seed = "INSERT INTO LOCATIONS VALUES (1);\n"
                    + "INSERT INTO DEPARTMENTS VALUES (2, 1);\n"
                    + "INSERT INTO EMPLOYEES VALUES (3, 2, NULL);\n"
                    + "INSERT INTO EMPLOYEES VALUES (4, 2, 3);\n"
                    + "INSERT INTO USER_CREDENTIALS VALUES (5, 3);\n";
            SutController controller = new DatabaseFakeH2SutController(connection, seed);
            controller.registerOrExecuteInitSqlCommandsIfNeeded();
            assertNotNull(controller.getSqlDatabaseSchema());
            List<String> tables = Arrays.asList("PUBLIC.USER_CREDENTIALS", "PUBLIC.EMPLOYEES", "PUBLIC.DEPARTMENTS", "PUBLIC.LOCATIONS");
            for (int round = 0; round < 5; round++) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("INSERT INTO EMPLOYEES VALUES (99, 2, 3)");
                }
                controller.resetDatabase(tables);
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM EMPLOYEES")) {
                    assertTrue(result.next());
                    assertEquals(2, result.getInt(1));
                }
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM USER_CREDENTIALS C JOIN EMPLOYEES E ON C.EMPLOYEE_ID=E.ID")) {
                    assertTrue(result.next());
                    assertEquals(1, result.getInt(1));
                }
            }
        }
    }
}
