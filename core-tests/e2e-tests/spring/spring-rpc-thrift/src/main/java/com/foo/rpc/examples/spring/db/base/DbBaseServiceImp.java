package com.foo.rpc.examples.spring.db.base;

import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class DbBaseServiceImp implements DbBaseService.Iface{

    @Autowired
    private DbBaseRepository repository;

    @Override
    public long create(DbBaseDto dto) throws TException {
        DbBaseEntity entity = new DbBaseEntity();
        entity.setName(dto.name);

        repository.save(entity);

        return entity.getId();
    }

    @Override
    public List<DbBaseDto> getAll() throws TException {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .map(e -> new DbBaseDto(e.getId(), e.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public DbBaseDto get(long id) throws TException {
        DbBaseEntity entity = repository.findById(id).orElse(null);

        if(entity == null){
            return null;
        }

        DbBaseDto dto = new DbBaseDto();
        dto.id = entity.getId();
        dto.name = entity.getName();

        return dto;
    }

    @Override
    public List<DbBaseDto> getByName(String name) throws TException {

        List<DbBaseEntity> entities = repository.findByName(name);
        if (entities.isEmpty()) {
            return null;
        }

        List<DbBaseDto> list = entities.stream()
                .map(e -> new DbBaseDto(e.getId(), e.getName()))
                .collect(Collectors.toList());

        return list;
    }
}
