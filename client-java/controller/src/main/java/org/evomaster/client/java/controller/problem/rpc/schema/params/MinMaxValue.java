package org.evomaster.client.java.controller.problem.rpc.schema.params;

public interface MinMaxValue<T extends Number> {

    T getMin();

    void setMin(T min);

    T getMax();

    void setMax(T max);

    boolean getMinInclusive();

    void setMinInclusive(boolean inclusive);

    boolean getMaxInclusive();

    void setMaxInclusive(boolean inclusive);
}
