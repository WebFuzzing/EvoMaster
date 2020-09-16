package org.evomaster.client.java.instrumentation.object.dtos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface Exclude {
}

public class DtoIgnore {

    private String a;
    private transient String b;
    private static String c;
    @Exclude private String d;

    public String getD() {
        return d;
    }

    public void setD(String d) {
        this.d = d;
    }

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }

    public static String getC() {
        return c;
    }

    public static void setC(String c) {
        DtoIgnore.c = c;
    }
}
