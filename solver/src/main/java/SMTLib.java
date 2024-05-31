import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SMTLib class Abstraction to generate SMT-LIB files
 */
public class SMTLib {

    // Two SMTLib are equal if they expose the same smt file
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SMTLib smtLib = (SMTLib) o;
        return Objects.equals(this.toString(), smtLib.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nodes);
    }

    public static class SMTNode {
        // Base class for all SMT nodes
    }

    public static class DeclareDatatype extends SMTNode {
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

    public static class DeclareConst extends SMTNode {
        private final String name;
        private final String type;

        public DeclareConst(String name, String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String toString() {
            return "(declare-const " + name + " " + type + ")";
        }
    }

    public static class Assert extends SMTNode {
        private final String condition;

        public Assert(String condition) {
            this.condition = condition;
        }

        @Override
        public String toString() {
            return "(assert " + condition + ")";
        }
    }

    public static class CheckSat extends SMTNode {
        @Override
        public String toString() {
            return "(check-sat)";
        }
    }

    public static class GetValue extends SMTNode {
        private final String variable;

        public GetValue(String variable) {
            this.variable = variable;
        }

        @Override
        public String toString() {
            return "(get-value (" + variable + "))";
        }
    }

    private final List<SMTNode> nodes = new ArrayList<>();

    public void addNode(SMTNode node) {
        nodes.add(node);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (SMTNode node : nodes) {
            sb.append(node.toString()).append("\n");
        }
        return sb.toString();
    }
}
