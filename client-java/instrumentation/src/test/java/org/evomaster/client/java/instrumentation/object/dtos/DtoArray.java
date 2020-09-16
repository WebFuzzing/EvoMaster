package org.evomaster.client.java.instrumentation.object.dtos;

import java.util.List;
import java.util.Set;

public class DtoArray {

    private String[] array;
    private Set<Integer> set;
    private Set set_raw;
    private List<Boolean> list;
    private List list_raw;

    public Set getSet_raw() {
        return set_raw;
    }

    public void setSet_raw(Set set_raw) {
        this.set_raw = set_raw;
    }

    public List getList_raw() {
        return list_raw;
    }

    public void setList_raw(List list_raw) {
        this.list_raw = list_raw;
    }

    public String[] getArray() {
        return array;
    }

    public void setArray(String[] array) {
        this.array = array;
    }

    public Set<Integer> getSet() {
        return set;
    }

    public void setSet(Set<Integer> set) {
        this.set = set;
    }

    public List<Boolean> getList() {
        return list;
    }

    public void setList(List<Boolean> list) {
        this.list = list;
    }
}
