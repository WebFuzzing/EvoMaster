package com.foo.rest.examples.spring.db.auth;


import com.foo.rest.examples.spring.db.auth.db.AuthProjectEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

@Service
public class AuthProjectService {

    @Autowired
    private EntityManager em;


    @Transactional
    public List<AuthProjectEntity> getForUser(String userId){

        TypedQuery<AuthProjectEntity> query = em.createQuery(
                "select p from AuthProjectEntity p where p.owner.userId=?1", AuthProjectEntity.class);
        query.setParameter(1, userId);

        return query.getResultList();
    }
}
