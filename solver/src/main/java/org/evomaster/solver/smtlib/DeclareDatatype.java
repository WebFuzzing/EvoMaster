package org.evomaster.solver.smtlib;

import java.util.List;

public class DeclareDatatype extends SMTNode {
    private final String name;
    private final List<DeclareConst> constructors;

    public DeclareDatatype(String name, List<DeclareConst> constructors) {
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

        for (DeclareConst constructor : constructors) {
            sb.append("(").append(constructor.name.toUpperCase()).append(" ").append(constructor.type).append(") ");
        }
        sb.append("))))\n");
        return sb.toString();
    }
}
