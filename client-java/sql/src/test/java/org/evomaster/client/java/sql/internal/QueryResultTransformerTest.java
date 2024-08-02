package org.evomaster.client.java.sql.internal;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * created by manzhang on 2024/7/30
 */
public class QueryResultTransformerTest {


    @Test
    public void testCartesianProductIntList(){
        List<Integer> setA = Arrays.asList(1,2,3);
        List<Integer> setB = Arrays.asList(7,8,9,0);

        List<List<Integer>> intValues = new ArrayList<List<Integer>>(){{
            add(setA);
            add(setB);
        }};

        List<List<Integer>> results = QueryResultTransformer.cartesianProduct(intValues);

        assertEquals(3 * 4, results.size());

        int index = 0;
        for(int indexA = 0; indexA < setA.size(); indexA++){
            for (int indexB = 0; indexB < setB.size(); indexB++){
                List<Integer> row = results.get(index);
                assertEquals(2, row.size());
                assertEquals(setA.get(indexA), row.get(0));
                assertEquals(setB.get(indexB), row.get(1));
                index++;
            }
        }
    }


    @Test
    public void testCartesianProductStringList(){
        List<String> setA = Arrays.asList("aaa","bbb");
        List<String> setB = Arrays.asList("nmt", "xyz");
        List<String> setC = Arrays.asList("foo", "bar");

        List<List<String>> intValues = new ArrayList<List<String>>(){{
            add(setA);
            add(setB);
            add(setC);
        }};

        List<List<String>> results = QueryResultTransformer.cartesianProduct(intValues);

        List<String> expected = Arrays.asList(
                "aaa,nmt,foo",
                "aaa,nmt,bar",
                "aaa,xyz,foo",
                "aaa,xyz,bar",
                "bbb,nmt,foo",
                "bbb,nmt,bar",
                "bbb,xyz,foo",
                "bbb,xyz,bar"
        );

        assertEquals(expected.size(), results.size());

        for (int i = 0; i < expected.size(); i++){
            assertEquals(expected.get(i), String.join(",",results.get(i)));
        }

    }
}
