package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Table("cassandra_template_test_dto")
public class CassandraTemplateTestDto {

    @PrimaryKey
    public UUID id;

    public String name;
    public int age;

    public CassandraTemplateTestDto() {
    }

    public CassandraTemplateTestDto(UUID id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }
}
