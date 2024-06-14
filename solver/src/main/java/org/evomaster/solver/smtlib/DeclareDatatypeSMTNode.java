package org.evomaster.solver.smtlib;

import java.util.List;

public class DeclareDatatypeSMTNode extends SMTNode {
    private final String name;
    private final List<DeclareConstSMTNode> constructors;

    public DeclareDatatypeSMTNode(String name, List<DeclareConstSMTNode> constructors) {
        this.name = name;
        this.constructors = constructors;
    }

    @Override
    public String toString() {
        String columnsConcat = constructors.stream()
                .map(c -> c.name.toLowerCase())
                .reduce((a, b) -> a + "-" + b)
                .orElse("");

        StringBuilder sb = new StringBuilder();
        sb.append("(declare-datatypes () ((")
                .append(name).append(" (")
                .append(columnsConcat).append(" ");

        for (DeclareConstSMTNode constructor : constructors) {
            sb.append("(").append(constructor.name.toUpperCase()).append(" ").append(constructor.type).append(") ");
        }
        sb.append("))))\n");
        return sb.toString();
    }
}
