package com.foo.rest.examples.spring.chainedheaderlocation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping(path = "/api/chl")
public class CHLRest {

    public static final Map<Integer, X> data = new ConcurrentHashMap<>();

    private final AtomicInteger counter = new AtomicInteger(0);


    @RequestMapping(
            path = "/x/{idx}/y/{idy}/z/{idz}/value",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity getZValue(
            @PathVariable("idx") int idx,
            @PathVariable("idy") int idy,
            @PathVariable("idz") int idz
    ){
        X x = data.get(idx);
        if(x == null){
            return ResponseEntity.status(404).build();
        }

        Y y = x.map.get(idy);
        if(y==null){
            return ResponseEntity.status(404).build();
        }

        Z z = y.map.get(idz);

        if(z == null){
            return ResponseEntity.status(404).build();
        }

        return ResponseEntity.status(200).build();
    }

    @RequestMapping(
            path = "/x",
            method = RequestMethod.POST
    )
    public ResponseEntity createX(){

        int index = counter.incrementAndGet();
        X x = new X();
        data.put(index, x);

        return ResponseEntity.created(URI.create("/api/chl/x/"+index)).build();
    }

    @RequestMapping(
            path = "/x/{idx}/y",
            method = RequestMethod.POST
    )
    public ResponseEntity createY( @PathVariable("idx") int idx){

        X x = data.get(idx);
        if(x == null){
            return ResponseEntity.status(404).build();
        }

        int index = counter.incrementAndGet();
        Y y = new Y();
        x.map.put(index, y);

        return ResponseEntity.created(URI.create("/api/chl/x/"+idx+"/y/"+index)).build();
    }


    @RequestMapping(
            path = "/x/{idx}/y/{idy}/z",
            method = RequestMethod.POST
    )
    public ResponseEntity createZ(
            @PathVariable("idx") int idx,
            @PathVariable("idy") int idy){

        X x = data.get(idx);
        if(x == null){
            return ResponseEntity.status(404).build();
        }

        Y y = x.map.get(idy);
        if(y == null){
            return ResponseEntity.status(404).build();
        }

        int index = counter.incrementAndGet();
        Z z = new Z();
        y.map.put(index, z);

        return ResponseEntity.created(URI.create("/api/chl/x/"+idx+"/y/"+idy+"/z/"+index)).build();
    }



    private class X{
        public final Map<Integer, Y> map = new ConcurrentHashMap<>();
    }

    private class Y{
        public final Map<Integer, Z> map = new ConcurrentHashMap<>();
    }

    private class Z {
        public String value;
    }
}
