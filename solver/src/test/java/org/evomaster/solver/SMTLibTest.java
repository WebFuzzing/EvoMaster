package org.evomaster.solver;

import org.evomaster.solver.smtlib.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SMTLibTest {
    static String resourcesFolder = System.getProperty("user.dir") + "/src/test/resources/";

    /**
     * Tests that the SMTLib toString method generates the expected string
     * @throws IOException when reading from expected response file
     */
    @Test
    public void SMTLibSelectToString() throws IOException {
        SMTLib smtLib = new SMTLib();
        smtLib.addNode(new DeclareDatatype("UsersRow", ImmutableList.of(
                new DeclareConst("ID", "Int"),
                new DeclareConst("NAME", "String"),
                new DeclareConst("AGE", "Int"),
                new DeclareConst("POINTS", "Int")
        )));
        smtLib.addNode(new DeclareConst("users1", "UsersRow"));
        smtLib.addNode(new DeclareConst("users2", "UsersRow"));
        smtLib.addNode(new CheckSat());
        smtLib.addNode(new GetValue("users1"));
        smtLib.addNode(new GetValue("users2"));

        // Read from file the response and compare
        String expected = readFromFileAsString(resourcesFolder + "select-from-users.smt");

        assertEquals(expected, smtLib.toString());
    }

    private static String readFromFileAsString(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
            line = br.readLine();
        }

        return sb.toString();
    }
}