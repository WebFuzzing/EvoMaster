package com.foo.rpc.examples.spring.fakemockobject.impl;

import com.foo.rpc.examples.spring.fakemockobject.generated.FakeDatabaseRow;
import com.foo.rpc.examples.spring.fakemockobject.generated.FakeMockObjectService;
import com.foo.rpc.examples.spring.fakemockobject.generated.FakeRetrieveData;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FakeMockObjectServiceImpl implements FakeMockObjectService.Iface{

    private final Map<Integer, FakeRetrieveData> retrieveDataMap = new HashMap<>();
    private final Map<Integer, FakeDatabaseRow> dbDataMap = new HashMap<>();


    @Override
    public String getFooFromExternalService(int id) throws TException {
        if (retrieveDataMap.containsKey(id)){
            FakeRetrieveData row = retrieveDataMap.get(id);
            return "EX:::"+(row.name==null?"untitled":row.name)+":::"+(row.info==null?"NONE":row.info);
        }
        return "NOT FOUND EX";
    }

    @Override
    public String getBarFromDatabase(int id) throws TException {
        if (dbDataMap.containsKey(id)){
            FakeDatabaseRow row = dbDataMap.get(id);
            return "DB:::"+(row.name==null?"untitled":row.name)+":::"+(row.info==null?"NONE":row.info);
        }
        return "NOT FOUND DB";
    }

    @Override
    public List<String> getAllBarFromDatabase() throws TException {
        if (dbDataMap.isEmpty())
            return Collections.emptyList();
        return dbDataMap.values().stream().map(s-> s.name).collect(Collectors.toList());
    }

    @Override
    public boolean backdoor(FakeRetrieveData exData, FakeDatabaseRow dbData) throws TException {
        if (exData == null && dbData == null){
            retrieveDataMap.clear();
            dbDataMap.clear();
            return true;
        }
        if (exData != null){
            retrieveDataMap.put(exData.id, exData);
        }
        if (dbData != null){
            dbDataMap.put(dbData.id, dbData);
        }


        return true;
    }
}
