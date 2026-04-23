package com.foo.rpc.examples.spring.db.directint;

import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DbDirectIntServiceImp implements DbDirectIntService.Iface{

    @Autowired
    private DbDirectIntRepository repository;

    @Override
    public void post() throws TException {
        DbDirectIntEntity entity = new DbDirectIntEntity();
        entity.setX(42);
        entity.setY(77);
        repository.save(entity);
    }

    @Override
    public int get(int x, int y) throws TException {
        List<DbDirectIntEntity> list = repository.findByXIsAndYIs(x, y);
        if (list.isEmpty()) {
            return 400;
        } else {
            return 200;
        }
    }
}
