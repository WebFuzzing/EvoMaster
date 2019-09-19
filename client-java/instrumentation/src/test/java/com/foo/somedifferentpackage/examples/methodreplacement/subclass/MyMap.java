package com.foo.somedifferentpackage.examples.methodreplacement.subclass;

import java.util.HashMap;

/**
 * Created by arcuri82 on 19-Sep-19.
 */
public class MyMap<K,V> extends HashMap<K,V> {

    public String lastCheckedKey = "";


    @Override
    public boolean containsKey(Object obj){

        lastCheckedKey = obj.toString();

        return super.containsKey(obj);
    }
}
