package com.foo.rest.examples.spring.expectations;

import com.foo.rest.examples.spring.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@RestController
public class ExpectationsSpringRest extends SwaggerConfiguration {
    public static void main(String[] args) {
        SpringApplication.run(ExpectationsSpringRest.class, args);
    }

    public class ExampleObject {
        private int id;
        private String name;
        private String description;

        public ExampleObject(int id, String name, String description){
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public ExampleObject(){
            this.name = "Unnamed";
            this.description = "Indescribable";
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
        public String getDescription() {return description;}
    }
    public class OtherExampleObject {
        private int id;
        private String namn;
        private String category;

        public OtherExampleObject(int id, String name, String category){
            this.id = id;
            this.namn = name;
            this.category = category;
        }

        public OtherExampleObject(){
            this.namn = "Unnamed";
            this.category = "None";
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return namn;
        }
    }

    @GetMapping(path = "/api/basicResponsesString/{s}")
    public String getString(
            @PathVariable("s") String succeeded
    ){
        return "Success! " + succeeded;
    }

    @GetMapping(path = "/api/basicResponsesNumeric/{s}")
    public int getNumeric(
            @PathVariable("s") int succeeded
    ){
        if(succeeded >= 0) return 42;
        else throw new IllegalArgumentException("I don't like negative numbers, and you gave me a " + succeeded);
    }

    // A test looking at getting the wrong input

    @GetMapping(path = "/api/basicInput/{s}")
    public int getInput(
            @PathVariable("s") int succeeded
    ){
        if(succeeded >= 0) return 42;
        else throw new IllegalArgumentException("I don't like negative numbers, and you gave me a " + succeeded);
    }

    // A test looking at wrong output type

    @GetMapping(path = "/api/responseObj/{s}")
    public OtherExampleObject getObject(
            @PathVariable("s") int succeeded
    ){
        if(succeeded >= 0) {
            return new OtherExampleObject(succeeded, "object_" + succeeded, "successes");
        }
        else{
            return new OtherExampleObject();
        }
    }

    // A test looking at wrong output structure

    @GetMapping(path = "/api/responseUnsupObj/{s}")
    public ExampleObject getUnsupObject(
            @PathVariable("s") int succeeded
    ){
        if( succeeded >= 0 ) {
            return new ExampleObject(succeeded, "validObject_" + succeeded, "successful");
        }
        else {
            return new ExampleObject();
        }
    }

    // A test looking at an array of returned objects

    @GetMapping(path = "/api/responseMultipleObjs/{s}")
    public ExampleObject[] getMultipleObjects(
            @PathVariable("s") int succeeded
    ){
        if( succeeded >= 0 ) {
            ExampleObject [] res = {
                    new ExampleObject(succeeded, "validObject_" + succeeded, "successful"),
                    new ExampleObject(succeeded+1, "validObject_" + (succeeded+1), "successful")
            };
            return res;
        }
        else {
            ExampleObject [] res = {
                    new ExampleObject(),
                    new ExampleObject()
            };
            return res;
        }
    }
}
