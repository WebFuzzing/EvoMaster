package org.evomaster.client.java.instrumentation.example.nonintegercomparisons;

/**
 * Created by arcuri82 on 02-Mar-20.
 */
public interface NIC_Example {

    int pos(long x, long y);

    int neg(long x, long y);

    int eq(long x, long y);


    int pos(double x, double y);

    int neg(double x, double y);

    int eq(double x, double y);


    int pos(float x, float y);

    int neg(float x, float y);

    int eq(float x, float y);

}
