package org.evomaster.client.java.controller.cassandra.dsl;

public interface CassandraSequenceDsl {

    /**
     * An insertion operation on a Cassandra table
     *
     * @param keyspaceName the target keyspace
     * @param tableName the target table in the keyspace
     * @return a statement in which it can be specified the values to add
     */
    CassandraStatementDsl insertInto(String keyspaceName, String tableName);
}
