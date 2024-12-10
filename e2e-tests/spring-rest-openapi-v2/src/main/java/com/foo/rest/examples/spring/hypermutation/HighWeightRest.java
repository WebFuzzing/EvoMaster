package com.foo.rest.examples.spring.hypermutation;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/highweight")
public class HighWeightRest {

    @PostMapping(value = "/differentWeight/{x}", consumes = "application/json")
    public ResponseEntity differentWeight(@PathVariable(name = "x") Integer x, @RequestParam(required = true) String y, @RequestBody HighWeightDto z){

        String response = "";
        if (x == 42){
            response = "x";
        }
        if (y.equalsIgnoreCase("foo")){
            response += "y";
        }
        if (z.f3.equals("2021-06-17")){
            response += "z";
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/lowWeightHighCoverage/{x}", consumes = "application/json")
    public ResponseEntity lowWeightHighCoverage(@PathVariable(name = "x") Integer x, @RequestParam(required = true) String y, @RequestBody HighWeightDto z){

        /*
            based on the implementation,
            modifying x should have higher impacts than y and z
         */
        String response = "";
        if (x == 42){
            response = "x1";
        }else if (x == 100){
            response = "x2";
        }else if(x == 500){
            response = "x3";
        }else if(x == 1000){
            response = "x4";
        }else if(x == 10000){
            response = "x5";
        }else{
            /*
                should have covered at least one branch related to x
                then reach the branch related to y and z
             */
            return ResponseEntity.notFound().build();
        }


        if (y.equalsIgnoreCase("foo")){
            response += "y";
        }
        if (z.f3.equals("2021-06-17")){
            response += "z";
        }

        return ResponseEntity.ok(response);
    }
}
