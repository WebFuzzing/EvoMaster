package com.foo.rest.examples.spring.taintcollection;

import net.thirdparty.taint.TaintCheckString;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
@RestController
@RequestMapping(path = "/api/taintcollection")
public class TaintCollectionRest {


    @GetMapping(path = "/contains")
    public String contains(@RequestParam(name = "value", required = true) String value){
        Set<String> set = new HashSet<>(Arrays.asList("bar12345", "foo12345"));
        if(! set.contains(value)){
            throw new IllegalArgumentException(":-(");
        }

        return "contains OK";
    }

    @GetMapping(path = "/containsAll")
    public String containsAll(
            @RequestParam(name = "x", required = true) String x,
            @RequestParam(name = "y", required = true) String y
            ){
        List<String> list = Arrays.asList("bar12345", "foo12345", "hello there", "tricky one");
        if(x.equals(y) || ! list.containsAll(Arrays.asList(x,y))){
            throw new IllegalArgumentException(":-(");
        }

        return "containsAll OK";
    }

    @GetMapping(path = "/remove")
    public String remove(@RequestParam(name = "x", required = true) String x){
        Set<String> set = new HashSet<>(Arrays.asList("bar12345", "foo12345"));
        if(! set.remove(x)){
            throw new IllegalArgumentException(":-(");
        }

        return "remove OK";
    }

    @GetMapping(path = "/removeAll")
    public String removeAll(
            @RequestParam(name = "x", required = true) String x,
            @RequestParam(name = "y", required = true) String y
    ){
        Set<String> set = new HashSet<>(Arrays.asList("bar12345", "foo12345", "hello there", "tricky one"));
        if(! set.removeAll(Arrays.asList(x,y))){
            throw new IllegalArgumentException(":-(");
        }

        return "removeAll OK";
    }


    @GetMapping(path = "/map/remove")
    public String mapRemove(
            @RequestParam(name = "x", required = true) String x,
            @RequestParam(name = "y", required = true) String y){

        Map<String, String> map = new HashMap<>();
        map.put("foo777", "bar888");

        if(! map.remove(x,y)){
            throw new IllegalArgumentException(":-(");
        }

        return "mapRemove OK";
    }

    @GetMapping(path = "/map/replace")
    public String mapReplace(
            @RequestParam(name = "x", required = true) String x,
            @RequestParam(name = "y", required = true) String y){

        Map<String, String> map = new HashMap<>();
        map.put("foo999", "ba222222r");

        if(! map.replace(x,y,"hello")){
            throw new IllegalArgumentException(":-(");
        }

        return "mapReplace OK";
    }

    @GetMapping(path = "/map/containsKey")
    public String mapContainsKey(
            @RequestParam(name = "x", required = true) String x,
            @RequestParam(name = "y", required = true) String y){

        Map<String, String> map = new HashMap<>();
        map.put("foo111111111", "bar67890");

        if(! map.containsKey(x)){
            throw new IllegalArgumentException(":-(");
        }

        return "mapContainsKey OK";
    }

    @GetMapping(path = "/map/containsValue")
    public String mapContainsValue(
            @RequestParam(name = "x", required = true) String x,
            @RequestParam(name = "y", required = true) String y){

        Map<String, String> map = new HashMap<>();
        map.put("foo111111111", "bar67890");

        if(! map.containsValue(y)){
            throw new IllegalArgumentException(":-(");
        }

        return "mapContainsValue OK";
    }

    @GetMapping(path = "/map/get")
    public String mapGet(
            @RequestParam(name = "x", required = true) String x,
            @RequestParam(name = "y", required = true) String y){

        Map<String, String> map = new HashMap<>();
        map.put("444foo444", "bar67890");

        if(map.get(x) == null){
            throw new IllegalArgumentException(":-(");
        }

        return "mapGet OK";
    }

    @GetMapping(path = "/map/getOrDefault")
    public String mapGetOrDefault(
            @RequestParam(name = "x", required = true) String x,
            @RequestParam(name = "y", required = true) String y){

        Map<String, String> map = new HashMap<>();
        map.put("4xyz0001", "bar67890");

        if(map.getOrDefault(x, "x").equals("x")){
            throw new IllegalArgumentException(":-(");
        }

        return "mapGetOrDefault OK";
    }
}
