package org.evomaster.client.java.controller.cassandra.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.CassandraInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.CassandraInsertionEntryDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * DSL (Domain Specific Language) for insertion operations on
 * a Cassandra database.
 */
public class CassandraDsl implements CassandraSequenceDsl, CassandraStatementDsl {

    private List<CassandraInsertionDto> list = new ArrayList<>();

    private CassandraDsl() {}

    /**
     * @return a DSL object to create Cassandra operations
     */
    public static CassandraSequenceDsl cassandra() {
        return new CassandraDsl();
    }

    /**
     * {@inheritDoc}
     * <p>
     * A new insertion is started, and it becomes the current one, ie the one on which the following
     * calls to {@link #d(String, String)} will add values.
     *
     * @throws IllegalArgumentException if the keyspace or the table is null or empty
     * @throws IllegalStateException    if this DSL was already closed with {@link #dtos()}
     */
    @Override
    public CassandraStatementDsl insertInto(String keyspaceName, String tableName) {

        checkDsl();

        if (keyspaceName == null || keyspaceName.isEmpty()) {
            throw new IllegalArgumentException("Unspecified keyspace");
        }

        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("Unspecified table");
        }

        CassandraInsertionDto dto = new CassandraInsertionDto();
        dto.keyspaceName = keyspaceName;
        dto.tableName = tableName;

        list.add(dto);

        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The value is added to the insertion currently being built, ie the one started by the last call
     * to {@link #insertInto(String, String)}.
     *
     * @throws NullPointerException     if the value is null
     * @throws IllegalArgumentException if the column is null or empty
     * @throws IllegalStateException    if this DSL was already closed with {@link #dtos()}
     */
    @Override
    public CassandraStatementDsl d(String columnName, String printableValue) {
        Objects.requireNonNull(printableValue);

        checkDsl();

        if (columnName == null || columnName.isEmpty()) {
            throw new IllegalArgumentException("Unspecified column");
        }

        CassandraInsertionEntryDto entry = new CassandraInsertionEntryDto();
        entry.columnName = columnName;
        entry.printableValue = printableValue;

        current().data.add(entry);

        return this;
    }

    @Override
    public CassandraSequenceDsl and() {
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This closes the DSL: any further call to {@link #insertInto(String, String)} or
     * {@link #d(String, String)} on this object will throw an {@link IllegalStateException}.
     */
    @Override
    public List<CassandraInsertionDto> dtos() {

        List<CassandraInsertionDto> tmp = list;
        list = null;

        return tmp;
    }

    private CassandraInsertionDto current() {
        return list.get(list.size() - 1);
    }

    private void checkDsl() {
        if (list == null) {
            throw new IllegalStateException("DTO was already built for this object");
        }
    }
}
