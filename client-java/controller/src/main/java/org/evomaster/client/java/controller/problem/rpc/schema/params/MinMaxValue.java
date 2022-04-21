package org.evomaster.client.java.controller.problem.rpc.schema.params;

public interface MinMaxValue<T extends Number> {

    public T getMin();

    public void setMin(T min);

    public T getMax();

    public void setMax(T max);

    public boolean getMinInclusive();

    public void setMinInclusive(boolean inclusive);

    public boolean getMaxInclusive();

    public void setMaxInclusive(boolean inclusive);
}
