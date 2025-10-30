package com.foo.rpc.examples.spring.fakemockobject.impl;

import com.foo.rpc.examples.spring.fakemockobject.generated.*;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FakeMockObjectServiceImpl implements FakeMockObjectService.Iface{

    private final Map<Integer, FakeRetrieveData> retrieveDataMap = new HashMap<>();
    private final Map<Integer, FakeDatabaseRow> dbDataMap = new HashMap<>();
    private FlagPerDay flagPerDay = new FlagPerDay();

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
    public boolean isExecutedToday() throws TException {
        Date today = new Date();

        if (flagPerDay != null && isSameDay(flagPerDay.latestExecution, today)){
            return flagPerDay.flag;
        }

        return false;

    }

    @Override
    public List<String> getAllBarFromDatabase() throws TException {
        if (dbDataMap.isEmpty())
            return Collections.emptyList();
        return dbDataMap.values().stream().map(s-> s.name).collect(Collectors.toList());
    }

    public void executeFlag(){
        LocalDate today = LocalDate.now();
        Date date = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (flagPerDay.latestExecution == null || flagPerDay.latestExecution.before(date)){
            flagPerDay.latestExecution = date;
            flagPerDay.flag = true;
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return false;
        }

        LocalDate localDate1 = date1.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate localDate2 = date2.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        return localDate1.equals(localDate2);
    }

    @Override
    public boolean backdoor(FakeRetrieveData exData, FakeDatabaseRow dbData, FakeScheduleTaskData scheduleTask) throws TException {
        if (exData == null && dbData == null && scheduleTask == null){
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

        if (scheduleTask != null && scheduleTask.name.equals("executeFlag")){
            executeFlag();
        }

        return true;
    }
}
