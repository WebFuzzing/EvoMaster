package com.foo.rpc.examples.spring.db.tree;

import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

@Service
public class DbTreeServiceImp implements DbTreeService.Iface{
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String NO_PARENT = "NO_PARENT";
    public static final String WITH_PARENT = "WITH_PARENT";


    @Autowired
    private EntityManager em;

    @Autowired
    private DbTreeRepository repository;

    @Override
    public String get(long id) throws TException {
        DbTreeEntity node = repository.findById(id).orElse(null);

        if(node == null){
            return NOT_FOUND;
        }

        Query query = em.createNativeQuery("SELECT * FROM db_tree_entity WHERE parent_id = " + id);
        List list = query.getResultList();
        Object withParent = list.isEmpty() ? null : list.get(0);

        if(withParent == null){
            return NO_PARENT;
        } else {
            return WITH_PARENT;
        }
    }
}
