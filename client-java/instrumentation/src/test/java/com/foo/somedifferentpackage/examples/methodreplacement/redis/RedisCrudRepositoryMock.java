package com.foo.somedifferentpackage.examples.methodreplacement.redis;

import org.evomaster.client.java.instrumentation.example.redis.RedisEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.redis.core.RedisKeyValueTemplate;

import java.util.Optional;

public class RedisCrudRepositoryMock implements CrudRepository<RedisEntity, String> {

    private final RedisKeyValueTemplate template;

    public RedisCrudRepositoryMock(RedisKeyValueTemplate template) {
        this.template = template;
    }

    @Override
    public <S extends RedisEntity> S save(S entity) {
        template.insert(entity);
        return entity;
    }

    @Override
    public Optional<RedisEntity> findById(String id) {
        return template.findById(id, RedisEntity.class);
    }

    @Override
    public Iterable<RedisEntity> findAll() {
        return template.findAll(RedisEntity.class);
    }

    @Override
    public <S extends RedisEntity> Iterable<S> saveAll(Iterable<S> iterable) { throw new UnsupportedOperationException(); }
    @Override
    public boolean existsById(String s) { throw new UnsupportedOperationException(); }
    @Override
    public Iterable<RedisEntity> findAllById(Iterable<String> strings) { throw new UnsupportedOperationException(); }
    @Override
    public long count() { throw new UnsupportedOperationException(); }
    @Override
    public void deleteById(String s) { throw new UnsupportedOperationException(); }
    @Override
    public void delete(RedisEntity entity) { throw new UnsupportedOperationException(); }
    @Override
    public void deleteAllById(Iterable<? extends String> iterable) { throw new UnsupportedOperationException(); }
    @Override
    public void deleteAll(Iterable<? extends RedisEntity> entities) { throw new UnsupportedOperationException(); }
    @Override
    public void deleteAll() { throw new UnsupportedOperationException(); }
}