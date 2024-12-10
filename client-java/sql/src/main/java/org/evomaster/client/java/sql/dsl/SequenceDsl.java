package org.evomaster.client.java.sql.dsl;

public interface SequenceDsl {

    /**
     * An insertion operation on the SQL Database (DB)
     *
     * @param tableName the target table in the DB
     * @param id an id for this insertion. Can be null.
     * @return a statement in which it can be specified the values to add
     */
    StatementDsl insertInto(String tableName, Long id);

    /**
     * An insertion operation on the SQL Database (DB)
     *
     * @param tableName the target table in the DB
     * @return a statement in which it can be specified the values to add
     */
    default StatementDsl insertInto(String tableName){
        return insertInto(tableName, null);
    }
}
