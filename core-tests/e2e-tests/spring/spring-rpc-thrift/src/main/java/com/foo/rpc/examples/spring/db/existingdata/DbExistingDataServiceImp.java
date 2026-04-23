package com.foo.rpc.examples.spring.db.existingdata;

import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

@Service
public class DbExistingDataServiceImp implements DbExistingDataService.Iface{

    @Autowired
    private EntityManager em;

    @Override
    public String get() throws TException {
        TypedQuery<ExistingDataEntityY> query = em.createQuery(
                "select y from ExistingDataEntityY y where y.x.id=42", ExistingDataEntityY.class);
        List<ExistingDataEntityY> data = query.getResultList();
        if (data.isEmpty()) return "EMPTY";
        return "NOT_EMPTY";
    }
}
