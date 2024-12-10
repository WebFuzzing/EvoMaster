package org.evomaster.solver.smtlib;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SMTLibTest {

    @Test
    public void testEqualsAndHashCode() {
        SMTLib smtLib1 = new SMTLib();
        SMTLib smtLib2 = new SMTLib();

        // Add the same nodes to both SMTLib instances
        smtLib1.addNode(new DeclareDatatypeSMTNode("Person", Arrays.asList(
                new DeclareConstSMTNode("name", "String"),
                new DeclareConstSMTNode("age", "Int")
        )));
        smtLib1.addNode(new DeclareConstSMTNode("person1", "Person"));

        smtLib2.addNode(new DeclareDatatypeSMTNode("Person", Arrays.asList(
                new DeclareConstSMTNode("name", "String"),
                new DeclareConstSMTNode("age", "Int")
        )));
        smtLib2.addNode(new DeclareConstSMTNode("person1", "Person"));

        // Ensure both SMTLib instances are equal
        assertEquals(smtLib1, smtLib2);
        assertEquals(smtLib1.hashCode(), smtLib2.hashCode());

        // Modify smtLib2 to make it different from smtLib1
        smtLib2.addNode(new DeclareConstSMTNode("person2", "Person"));

        // Ensure the modified SMTLib instances are not equal
        assertNotEquals(smtLib1, smtLib2);
        assertNotEquals(smtLib1.hashCode(), smtLib2.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        SMTLib smtLib1 = new SMTLib();
        SMTLib smtLib2 = new SMTLib();

        smtLib1.addNode(new DeclareDatatypeSMTNode("Person", Arrays.asList(
                new DeclareConstSMTNode("name", "String"),
                new DeclareConstSMTNode("age", "Int")
        )));
        smtLib1.addNode(new DeclareConstSMTNode("person1", "Person"));

        smtLib2.addNode(new DeclareDatatypeSMTNode("Person", Arrays.asList(
                new DeclareConstSMTNode("name", "String"),
                new DeclareConstSMTNode("age", "Int")
        )));
        smtLib2.addNode(new DeclareConstSMTNode("person1", "Person"));

        // Ensure hashCode consistency with equals
        assertEquals(smtLib1, smtLib2);
        assertEquals(smtLib1.hashCode(), smtLib2.hashCode());
    }

    @Test
    public void testDeclareDatatypeSMTNode() {
        DeclareDatatypeSMTNode node = new DeclareDatatypeSMTNode("Person", Arrays.asList(
                new DeclareConstSMTNode("name", "String"),
                new DeclareConstSMTNode("age", "Int")
        ));
        String expected = "(declare-datatypes () ((Person (name-age (NAME String) (AGE Int) ))))\n";
        assertEquals(expected, node.toString());
    }


    @Test
    public void testSMTLibWithMultipleNodes() {
        SMTLib smtLib = new SMTLib();
        smtLib.addNode(new DeclareDatatypeSMTNode("Person", Arrays.asList(
                new DeclareConstSMTNode("name", "String"),
                new DeclareConstSMTNode("age", "Int")
        )));

        List<SMTNode> newNodes = Arrays.asList(
                new DeclareConstSMTNode("person1", "Person"),
                new DeclareConstSMTNode("person2", "Person"),
                new CheckSatSMTNode(),
                new GetValueSMTNode("person1"),
                new GetValueSMTNode("person2")
        );
        smtLib.addNodes(newNodes);

        String expected = "(declare-datatypes () ((Person (name-age (NAME String) (AGE Int) ))))\n\n" +
                "(declare-const person1 Person)\n" +
                "(declare-const person2 Person)\n" +
                "(check-sat)\n" +
                "(get-value (person1))\n" +
                "(get-value (person2))\n";

        assertEquals(expected, smtLib.toString());
    }
}