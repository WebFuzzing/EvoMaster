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
