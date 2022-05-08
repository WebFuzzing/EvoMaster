package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;

public interface NumericConstraintBase<T extends Number> {

    T getMin();

    void setMin(T min);

    T getMax();

    void setMax(T max);

    boolean getMinInclusive();

    void setMinInclusive(boolean inclusive);

    boolean getMaxInclusive();

    void setMaxInclusive(boolean inclusive);


    Integer getPrecision();

    void setPrecision(Integer precision);

    Integer getScale();

    void setScale(Integer scale);

    default void handleConstraintsInCopy(NamedTypedValue copy){
        if (!(copy instanceof NumericConstraintBase))
            throw new IllegalArgumentException("ERROR: cannot handle the copy which does not implement NumericConstraintBase");

        ((NumericConstraintBase) copy).setMax(getMax());
        ((NumericConstraintBase) copy).setMin(getMin());
        ((NumericConstraintBase) copy).setMaxInclusive(getMaxInclusive());
        ((NumericConstraintBase) copy).setMinInclusive(getMinInclusive());
        ((NumericConstraintBase) copy).setPrecision(getPrecision());
        ((NumericConstraintBase) copy).setScale(getScale());
    }

    default void handleConstraintsInCopyDto(ParamDto copy){
        if (getMax() != null)
            copy.maxValue = getMax().toString();
        if (getMin() != null)
            copy.minValue = getMin().toString();
        copy.maxInclusive = getMaxInclusive();
        copy.minInclusive = getMinInclusive();
        copy.precision = getPrecision();
        copy.scale = getScale();
    }
}
