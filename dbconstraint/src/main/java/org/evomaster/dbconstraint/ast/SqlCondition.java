package org.evomaster.dbconstraint.ast;

import java.util.List;

public abstract class SqlCondition {

    public abstract String toSql();

    public abstract <K, V> K accept(SqlConditionVisitor<K, V> visitor, V argument);

    public final String toString() {
        return toSql();
    }

    protected static String join(List<String> parts, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

}
