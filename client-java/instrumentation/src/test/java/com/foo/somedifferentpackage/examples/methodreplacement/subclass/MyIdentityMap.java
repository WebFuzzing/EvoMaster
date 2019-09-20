package com.foo.somedifferentpackage.examples.methodreplacement.subclass;

import java.util.IdentityHashMap;

/**
 * Created by arcuri82 on 19-Sep-19.
 */
public class MyIdentityMap<K,V> extends IdentityHashMap<K,V> {

    public String lastCheckedKey = "";


    @Override
    public boolean containsKey(Object obj){

        lastCheckedKey = obj.toString();

        return super.containsKey(obj);
    }
}
