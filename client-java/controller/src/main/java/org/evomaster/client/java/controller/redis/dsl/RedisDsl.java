package org.evomaster.client.java.controller.redis.dsl;

import org.evomaster.client.java.controller.api.dto.database.operations.RedisInsertionDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DSL (Domain Specific Language) for insertions on
 * the Redis Database.
 */
public class RedisDsl implements RedisSequenceDsl, RedisStatementDsl {

    private List<RedisInsertionDto> list = new ArrayList<>();
    private final List<RedisInsertionDto> previousInsertionDtos = new ArrayList<>();

    private RedisDsl() {}

    private RedisDsl(List<RedisInsertionDto>... previous) {
        if (previous != null && previous.length > 0) {
            Arrays.stream(previous).forEach(previousInsertionDtos::addAll);
        }
    }

    /**
     * @return a DSL object to create Redis operations
     */
    public static RedisSequenceDsl redis() {
        return new RedisDsl();
    }

    /**
     * @param previous a DSL object which is executed in the front of this
     * @return a DSL object to create Redis operations
     */
    @SafeVarargs
    public static RedisSequenceDsl redis(List<RedisInsertionDto>... previous) {
        return new RedisDsl(previous);
    }

    @Override
    public RedisStatementDsl set(String key, String value) {
        checkDsl();
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Unspecified key");
        }
        RedisInsertionDto dto = new RedisInsertionDto();
        dto.command = "SET";
        dto.key = key;
        dto.value = value;
        list.add(dto);
        return this;
    }

    @Override
    public RedisStatementDsl hset(String key, String field, String value) {
        checkDsl();
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Unspecified key");
        }
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Unspecified field");
        }
        RedisInsertionDto dto = new RedisInsertionDto();
        dto.command = "HSET";
        dto.key = key;
        dto.field = field;
        dto.value = value;
        list.add(dto);
        return this;
    }

    @Override
    public RedisStatementDsl sadd(String key, String member) {
        checkDsl();
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Unspecified key");
        }
        if (member == null || member.isEmpty()) {
            throw new IllegalArgumentException("Unspecified member");
        }
        RedisInsertionDto dto = new RedisInsertionDto();
        dto.command = "SADD";
        dto.key = key;
        dto.value = member;
        list.add(dto);
        return this;
    }

    @Override
    public RedisSequenceDsl and() {
        return this;
    }

    @Override
    public List<RedisInsertionDto> dtos() {
        List<RedisInsertionDto> tmp = list;
        list = null;
        return tmp;
    }

    private void checkDsl() {
        if (list == null) {
            throw new IllegalStateException("DTO was already built for this object");
        }
    }
}