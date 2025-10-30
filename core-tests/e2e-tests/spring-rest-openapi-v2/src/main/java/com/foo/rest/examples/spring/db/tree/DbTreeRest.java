package com.foo.rest.examples.spring.db.tree;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

@RestController
@RequestMapping(path = "/api/db/tree")
public class DbTreeRest {

    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String NO_PARENT = "NO_PARENT";
    public static final String WITH_PARENT = "WITH_PARENT";


    @Autowired
    private EntityManager em;

    @Autowired
    private DbTreeRepository repository;


    //TODO put back once we deal with ON predicates in JOIN
    //actually best to have it as well, but with a different Entity
//    @GetMapping(path = "/{id}")
//    public String get(@PathVariable("id") Long id){
//
//        DbTreeEntity node = repository.findById(id).orElse(null);
//
//        if(node == null){
//            return NOT_FOUND;
//        }
//
//        if(node.getParent() == null){
//            return NO_PARENT;
//        } else {
//            return WITH_PARENT;
//        }
//    }


    @GetMapping(path = "/{id}")
    public String get(@PathVariable("id") Long id){

        DbTreeEntity node = repository.findById(id).orElse(null);

        if(node == null){
            return NOT_FOUND;
        }

//        TypedQuery<DbTreeEntity> query = em.createQuery(
//                "select n from DbTreeEntity n where n.parent.id = " + id, DbTreeEntity.class);
//        DbTreeEntity withParent = query.getSingleResult();

        //db_tree_entity (id bigint not null, parent_id bigint
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
