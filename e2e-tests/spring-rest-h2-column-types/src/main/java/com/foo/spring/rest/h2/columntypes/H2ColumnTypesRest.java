package com.foo.spring.rest.h2.columntypes;

//public class H2DataTypesAppJ {
//}


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by jgaleotti on 23-Jun-22.
 */
@RestController
@RequestMapping(path = "/api/h2/")
public class H2ColumnTypesRest {

    @Autowired
    private EntityManager em;


    @GetMapping("/charactertypes")
    public ResponseEntity<Void> getCharacterTypes() {
        Query query = em.createNativeQuery(
                "select (1) from characterTypes where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/charactervaryingtypes")
    public ResponseEntity<Void> getCharacterVaryingTypes() {
        Query query = em.createNativeQuery(
                "select (1) from characterVaryingTypes where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }


    @GetMapping("/characterlargeobjecttypes")
    public ResponseEntity<Void> getCharacterLargeObjectTypes() {
        Query query = em.createNativeQuery(
                "select (1) from characterLargeObjectTypes where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/binarytypes")
    public ResponseEntity<Void> getBinaryTypes() {
        Query query = em.createNativeQuery(
                "select (1) from binaryTypes where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    //numericTypes
    @GetMapping("/numerictypes")
    public ResponseEntity<Void> getNumericTypes() {
        Query query = em.createNativeQuery(
                "select (1) from numericTypes where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/varcharignorecasetype")
    public ResponseEntity<Void> getVarCharIgnoreCaseTypes() {
        Query query = em.createNativeQuery(
                "select (1) from varcharIgnoreCaseType where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/datetimetypes")
    public ResponseEntity<Void> getDateTimeTypes() {
        Query query = em.createNativeQuery(
                "select (1) from dateTimeTypes where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/jsontype")
    public ResponseEntity<Void> getJsonType() {
        Query query = em.createNativeQuery(
                "select (1) from jsontype where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/uuidtype")
    public ResponseEntity<Void> getUUIDType() {
        Query query = em.createNativeQuery(
                "select (1) from uuidType where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/intervaltypes")
    public ResponseEntity<Void> getIntervalTypes() {
        Query query = em.createNativeQuery(
                "select (1) from intervalTypes where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/javaobjecttypes")
    public ResponseEntity<Void> getJavaObjectTypes() {
        Query query = em.createNativeQuery(
                "select (1) from javaObjectTypes where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/geometrytypes")
    public ResponseEntity<Void> getGeometryTypes() {
        Query query = em.createNativeQuery(
                "select (1) from geometryTypes where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/enumtype")
    public ResponseEntity<Void> getEnumType() {
        Query query = em.createNativeQuery(
                "select (1) from enumType where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/createtypeasenum")
    public ResponseEntity<Void> getCreateTypeAsEnum() {
        Query query = em.createNativeQuery(
                "select (1) from createTypeAsEnumTable where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }

    @GetMapping("/arraytypes")
    public ResponseEntity<Void> getArrayTypes() {
        Query query = em.createNativeQuery(
                "select (1) from arrayTypes where dummyColumn>0");
        List<?> data = query.getResultList();

        if(data.isEmpty()) {
            return ResponseEntity.status(400).build();
        } else {
            return ResponseEntity.status(200).build();
        }
    }
}

