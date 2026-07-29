package org.evomaster.client.java.controller.cassandra.dsl;

import org.evomaster.client.java.controller.cassandra.insertions.model.CassandraInsertionDto;
import org.evomaster.client.java.controller.cassandra.insertions.model.CassandraInsertionEntryDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DSL (Domain Specific Language) for insertion operations on
 * a Cassandra database.
 */
public class CassandraDsl implements CassandraSequenceDsl, CassandraStatementDsl {

    private List<CassandraInsertionDto> list = new ArrayList<>();

    private final List<CassandraInsertionDto> previousInsertionDtos = new ArrayList<>();

    private CassandraDsl() {
    }

    private CassandraDsl(List<CassandraInsertionDto>... previous) {
        if (previous != null && previous.length > 0) {
            Arrays.stream(previous).forEach(previousInsertionDtos::addAll);
        }
    }

    /**
     * @return a DSL object to create Cassandra operations
     */
    public static CassandraSequenceDsl cassandra() {
        return new CassandraDsl();
    }

    /**
     * @param previous a DSL object which is executed in the front of this
     * @return a DSL object to create Cassandra operations
     */
    @SafeVarargs
    public static CassandraSequenceDsl cassandra(List<CassandraInsertionDto>... previous) {
        return new CassandraDsl(previous);
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
