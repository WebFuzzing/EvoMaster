package org.evomaster.client.java.instrumentation.example.methodreplacement;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Created by arcuri82 on 27-Jun-19.
 */
public interface TestabilityExc {

    int parseInt(String input);

    LocalDate parseLocalDate(String input);

    boolean after(Date instance, Date when);

    boolean before(Date instance, Date when);

    boolean equals(Date instance, Date other);

    boolean isEmpty(Collection instance);

    boolean contains(Collection instance, Object o);

    boolean containsKey(Map instance, Object o);

    boolean objectEquals(Object left, Object right);

}
