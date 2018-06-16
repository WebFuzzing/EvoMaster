package org.evomaster.clientJava.controller.db.dsl;

public interface SequenceDsl {

    /**
     * An insertion operation on the SQL Database (DB)
     *
     * @param tableName the target table in the DB
     * @param id an id for this insertion. Can be null.
     * @return
     */
    StatementDsl insertInto(String tableName, Long id);

    /**
     * An insertion operation on the SQL Database (DB)
     *
     * @param tableName the target table in the DB
     * @return
     */
    default StatementDsl insertInto(String tableName){
        return insertInto(tableName, null);
    }
}
