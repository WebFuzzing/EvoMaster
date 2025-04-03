package com.foo.spring.rest.mysql.entity;

import org.springframework.stereotype.Service;

@Service
public class MyEntityServiceImpl implements MyEntityService {

    private final MyEntityRepository myEntityRepository;

    public MyEntityServiceImpl(MyEntityRepository myEntityRepository) {
        this.myEntityRepository = myEntityRepository;
    }

    @Override
    public MyEntity getMyEntityById(Long id) {
        return myEntityRepository.findById(id).orElse(null);
    }
}
