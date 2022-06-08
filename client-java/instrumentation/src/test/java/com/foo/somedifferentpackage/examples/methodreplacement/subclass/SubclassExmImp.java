package com.foo.somedifferentpackage.examples.methodreplacement.subclass;

import com.google.common.collect.HashBasedTable;
import org.evomaster.client.java.instrumentation.example.methodreplacement.subclass.SubclassExm;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by arcuri82 on 19-Sep-19.
 */
public class SubclassExmImp implements SubclassExm {


    @Override
    public String exe() {

        MyMap<String, Integer> map = new MyMap<>();

        //this call should not be replaced
        map.containsKey("foo");


        //this call should not be replaced
        MyIdentityMap<String, String> ident = new MyIdentityMap<>();
        ident.containsKey("bar");

        //replaced
        Map<String,String> regular = new HashMap<>();
        regular.containsKey("123");

        // this is as well replaced.
        Map<String, Integer> subclass = new MyMap<>();
        subclass.containsKey("456");

        return map.lastCheckedKey + ident.lastCheckedKey + ((MyMap)subclass).lastCheckedKey;
    }

    @Override
    public String guavaMap() {
        HashBasedTable t = HashBasedTable.create();
        t.put("a","b","c");
        t.get(TaintInputName.getTaintName(0), "bar");
        return "ok";
    }
}
