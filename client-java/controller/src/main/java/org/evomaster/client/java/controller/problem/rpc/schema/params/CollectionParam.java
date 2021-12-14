package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.CollectionType;

public abstract class CollectionParam<V> extends NamedTypedValue<CollectionType, V>{

    private Integer minSize;
    private Integer maxSize;

    public CollectionParam(String name, CollectionType type) {
        super(name, type);
    }


    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        if (this.minSize != null && this.minSize >= minSize)
            return;
        this.minSize = minSize;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.maxSize = Long.valueOf(maxSize);
        dto.minSize = Long.valueOf(minSize);
        return dto;
    }
}
